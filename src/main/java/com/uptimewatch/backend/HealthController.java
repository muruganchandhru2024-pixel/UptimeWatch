
package com.uptimewatch.backend;

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

    @GetMapping("/api/check")
    public Map<String, Object> check(
            @RequestParam(defaultValue = "https://example.com") String url) {

        long start = System.nanoTime();
        int httpStatus = 0;
        String status = "DOWN";

        try {
            URI uri = URI.create(url);

            if ((! "https".equalsIgnoreCase(uri.getScheme())
                    && ! "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("Invalid URL");
            }

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
