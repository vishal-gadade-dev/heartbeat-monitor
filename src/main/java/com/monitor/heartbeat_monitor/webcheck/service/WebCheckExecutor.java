package com.monitor.heartbeat_monitor.webcheck.service;

import com.monitor.heartbeat_monitor.webcheck.constant.ProbeMessages;
import com.monitor.heartbeat_monitor.webcheck.dto.ProbeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WebCheckExecutor {

    private final HttpClient httpClient;

    public WebCheckExecutor() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public CompletableFuture<ProbeResult> probeAsync(String targetUrl) {
        long start = System.currentTimeMillis();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .orTimeout(5, TimeUnit.SECONDS)   // hard body-read deadline
                    .handle((response, throwable) -> {
                        long duration = System.currentTimeMillis() - start;

                        if (throwable != null) {
                            Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
                            if (cause instanceof HttpTimeoutException
                                    || cause instanceof java.util.concurrent.TimeoutException
                                    || duration >= 5000) {
                                return ProbeResult.builder()
                                        .up(false)
                                        .statusCode(null)
                                        .responseTimeMs(duration)
                                        .message(String.format(ProbeMessages.NETWORK_TIMEOUT, "5s hard deadline exceeded"))
                                        .build();
                            }
                            return ProbeResult.builder()
                                    .up(false)
                                    .statusCode(null)
                                    .responseTimeMs(duration)
                                    .message(String.format(ProbeMessages.PROBE_FAILED, cause.getMessage()))
                                    .build();
                        }

                        int code = response.statusCode();
                        boolean withinLimit = duration <= 5000;
                        boolean is2xx = code >= 200 && code < 300;
                        boolean isUp = is2xx && withinLimit;

                        String message;
                        if (!withinLimit) {
                            message = String.format(ProbeMessages.RESPONSE_TIME_EXCEEDED, duration);
                        } else if (is2xx) {
                            message = String.format(ProbeMessages.HTTP_OK, code);
                        } else {
                            message = String.format(ProbeMessages.HTTP_NON_2XX, code);
                        }

                        return ProbeResult.builder()
                                .up(isUp)
                                .statusCode(code)
                                .responseTimeMs(duration)
                                .message(message)
                                .build();
                    });

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            return CompletableFuture.completedFuture(
                    ProbeResult.builder()
                            .up(false)
                            .statusCode(null)
                            .responseTimeMs(duration)
                            .message(String.format(ProbeMessages.PROBE_FAILED, e.getMessage()))
                            .build()
            );
        }
    }
}
