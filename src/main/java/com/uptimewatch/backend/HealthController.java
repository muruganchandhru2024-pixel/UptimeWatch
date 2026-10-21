
package com.uptimewatch.backend;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final MonitoringRecordRepository repository;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public HealthController(MonitoringRecordRepository repository) {
        this.repository = repository;
    }

    private boolean isPublicAddress(InetAddress ip) {
        if (ip.isAnyLocalAddress()
                || ip.isLoopbackAddress()
                || ip.isLinkLocalAddress()
                || ip.isSiteLocalAddress()
                || ip.isMulticastAddress()) {
            return false;
        }

        byte[] b = ip.getAddress();

        if (ip instanceof Inet4Address) {
            int a = b[0] & 0xff;
            int c = b[1] & 0xff;
            int d = b[2] & 0xff;
            int e = b[3] & 0xff;

            // Unspecified, private, loopback, multicast and reserved ranges.
            if (a == 0 || a == 10 || a == 127 || a >= 224) {
                return false;
            }

            // Carrier-grade NAT: 100.64.0.0/10
            if (a == 100 && c >= 64 && c <= 127) {
                return false;
            }

            // Link-local and AWS instance metadata range.
            if (a == 169 && c == 254) {
                return false;
            }

            // Private IPv4 ranges.
            if (a == 172 && c >= 16 && c <= 31) {
                return false;
            }
            if (a == 192 && c == 168) {
                return false;
            }

            // Special-purpose and documentation ranges.
            if (a == 192 && c == 0 && d == 0) return false;
            if (a == 192 && c == 0 && d == 2) return false;
            if (a == 192 && c == 88 && d == 99) return false;
            if (a == 198 && (c == 18 || c == 19)) return false;
            if (a == 198 && c == 51 && d == 100) return false;
            if (a == 203 && c == 0 && d == 113) return false;

            // Broadcast address.
            if (a == 255 && c == 255 && d == 255 && e == 255) {
                return false;
            }

            return true;
        }

        if (ip instanceof Inet6Address) {
            // IPv4-mapped IPv6 addresses are not accepted.
            if (ip instanceof Inet6Address
                    && ((Inet6Address) ip).getScopeId() != 0) {
                return false;
            }

            // Only global-unicast IPv6 (2000::/3) is considered.
            if ((b[0] & 0xe0) != 0x20) {
                return false;
            }

            // Reject documentation and transition/tunnelling ranges.
            if ((b[0] & 0xff) == 0x20
                    && (b[1] & 0xff) == 0x01
                    && (b[2] & 0xff) == 0x0d
                    && (b[3] & 0xff) == 0xb8) {
                return false; // 2001:db8::/32
            }

            if ((b[0] & 0xff) == 0x20
                    && (b[1] & 0xff) == 0x02) {
                return false; // 6to4
            }

            if ((b[0] & 0xff) == 0x20
                    && (b[1] & 0xff) == 0x01
                    && (b[2] & 0xff) == 0x00
                    && (b[3] & 0xff) == 0x00) {
                return false; // Teredo
            }

            return true;
        }

        return false;
    }

    private URI validateUrl(String rawUrl) throws Exception {
        if (rawUrl == null || rawUrl.isBlank() || rawUrl.length() > 2048) {
            throw new IllegalArgumentException("Invalid URL");
        }

        URI uri = URI.create(rawUrl.trim()).parseServerAuthority();

        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme == null
                || (!scheme.equalsIgnoreCase("http")
                    && !scheme.equalsIgnoreCase("https"))
                || host == null
                || uri.getUserInfo() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("Invalid URL");
        }

        host = host.toLowerCase(Locale.ROOT);

        // Reject local hostnames and local-only suffixes.
        if (host.equals("localhost")
                || host.endsWith(".localhost")
                || host.endsWith(".local")
                || host.endsWith(".")) {
            throw new IllegalArgumentException("Local hostname is not allowed");
        }

        // Explicit AWS metadata endpoint protection.
        if (host.equals("169.254.169.254")
                || host.equals("169.254.170.2")
                || host.equals("[::1]")
                || host.equals("::1")) {
            throw new IllegalArgumentException("Metadata/local address is not allowed");
        }

        // Resolve every DNS result and reject the hostname if any result
        // points to a non-public address.
        InetAddress[] addresses = InetAddress.getAllByName(host);

        if (addresses.length == 0) {
            throw new IllegalArgumentException("Host could not be resolved");
        }

        for (InetAddress address : addresses) {
            if (!isPublicAddress(address)) {
                throw new IllegalArgumentException(
                        "Private or unsafe destination is not allowed");
            }
        }

        return uri;
    }

    @GetMapping("/api/check")
    public Map<String, Object> check(
            @RequestParam(defaultValue = "https://example.com") String url) {

        long start = System.nanoTime();
        int httpStatus = 0;
        String status = "DOWN";

        try {
            URI uri = validateUrl(url);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "UptimeWatch/1.0")
                    .GET()
                    .build();

            HttpResponse<Void> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.discarding());

            httpStatus = response.statusCode();
            status = httpStatus >= 200 && httpStatus < 400
                    ? "UP" : "DOWN";

        } catch (Exception e) {
            // Do not expose internal validation/network error details.
            status = "DOWN";
            httpStatus = 0;
        }

        long elapsed = (System.nanoTime() - start) / 1_000_000;

        MonitoringRecord record = new MonitoringRecord();
        record.setUrl(url);
        record.setStatus(status);
        record.setHttpStatus(httpStatus);
        record.setResponseTimeMs(elapsed);

        MonitoringRecord saved = repository.save(record);

        return Map.of(
                "id", saved.getId(),
                "url", url,
                "status", status,
                "httpStatus", httpStatus,
                "responseTimeMs", elapsed
        );
    }

    @GetMapping("/api/history")
    public List<MonitoringRecord> getHistory() {
        return repository.findTop20ByOrderByCheckedAtDesc();
    }
}
