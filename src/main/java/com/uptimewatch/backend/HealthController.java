
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

            if (a == 0 || a == 10 || a == 127 || a >= 224) {
                return false;
            }
            if (a == 100 && c >= 64 && c <= 127) return false;
            if (a == 169 && c == 254) return false;
            if (a == 172 && c >= 16 && c <= 31) return false;
            if (a == 192 && c == 168) return false;
            if (a == 192 && c == 0 && d == 0) return false;
            if (a == 192 && c == 0 && d == 2) return false;
            if (a == 198 && (c == 18 || c == 19)) return false;
            if (a == 198 && c == 51 && d == 100) return false;
            if (a == 203 && c == 0 && d == 113) return false;

            return true;
        }

        if (ip instanceof Inet6Address) {
            // Reject IPv6 unique-local addresses (fc00::/7).
            return (b[0] & 0xfe) != 0xfc;
        }

        return false;
    }

    private URI validateUrl(String url) throws Exception {
        URI uri = URI.create(url.trim()).parseServerAuthority();
        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme == null
                || (!scheme.equalsIgnoreCase("https")
                    && !scheme.equalsIgnoreCase("http"))
                || host == null
                || uri.getUserInfo() != null
                || host.equalsIgnoreCase("localhost")
                || host.endsWith(".localhost")
                || host.endsWith(".local")) {
            throw new IllegalArgumentException("Invalid or unsafe URL");
        }

        // Reject the AWS IPv4 instance metadata endpoint explicitly.
        if (host.equals("169.254.169.254")) {
            throw new IllegalArgumentException("Unsafe URL");
        }

        InetAddress[] addresses = InetAddress.getAllByName(host);

        if (addresses.length == 0) {
            throw new IllegalArgumentException("Host could not be resolved");
        }

        // Reject the host if ANY DNS result is non-public.
        for (InetAddress address : addresses) {
            if (!isPublicAddress(address)) {
                throw new IllegalArgumentException("Private or unsafe host");
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
                    .GET()
                    .build();

            HttpResponse<Void> response = client.send(
                    request, HttpResponse.BodyHandlers.discarding());

            httpStatus = response.statusCode();
            status = httpStatus >= 200 && httpStatus < 400
                    ? "UP" : "DOWN";

        } catch (Exception e) {
            // Keep the result DOWN if the website cannot be checked.
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
