package com.monitor.heartbeat_monitor.alert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.heartbeat_monitor.alert.enums.AlertStatus;
import com.monitor.heartbeat_monitor.alert.model.Alert;
import com.monitor.heartbeat_monitor.alert.repository.AlertRepository;
import com.monitor.heartbeat_monitor.check.model.Check;
import com.monitor.heartbeat_monitor.common.constant.LogMessages;
import com.monitor.heartbeat_monitor.delivery.policy.BackoffPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AlertService(AlertRepository alertRepository, ObjectMapper objectMapper) {
        this.alertRepository = alertRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Transactional
    public Alert createAlertRecord(Check check, String eventType, String message) {
        String payload;
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("check_id", check.getId());
            body.put("check_name", check.getName());
            body.put("kind", check.getKind().name().toLowerCase());
            body.put("status", eventType);
            body.put("message", message);
            body.put("timestamp", LocalDateTime.now().toString());
            payload = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            payload = "{\"check_id\":" + check.getId() + ",\"status\":\"" + eventType + "\",\"message\":\"" + message + "\"}";
        }

        Alert alert = Alert.builder()
                .checkId(check.getId())
                .checkName(check.getName())
                .alertUrl(check.getAlertUrl())
                .eventType(eventType)
                .payload(payload)
                .retryCount(0)
                .maxRetries(5)
                .nextRetryAt(LocalDateTime.now())
                .status(AlertStatus.PENDING)
                .build();

        return alertRepository.save(alert);
    }

    public void dispatch(Alert alert) {
        dispatchAsync(alert);
    }

    public void dispatchAsync(Alert alert) {
        Alert currentAlert = alertRepository.findById(alert.getId()).orElse(null);
        if (currentAlert == null
                || AlertStatus.DELIVERED.equals(currentAlert.getStatus())
                || AlertStatus.FAILED_PERMANENTLY.equals(currentAlert.getStatus())) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(currentAlert.getAlertUrl()))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(currentAlert.getPayload()))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            handleSuccess(currentAlert.getId());
                        } else {
                            handleFailure(currentAlert.getId(), "HTTP non-2xx response: " + response.statusCode());
                        }
                    })
                    .exceptionally(throwable -> {
                        handleFailure(currentAlert.getId(), throwable.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            handleFailure(currentAlert.getId(), e.getMessage());
        }
    }

    private void handleSuccess(Long alertId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.setStatus(AlertStatus.DELIVERED);
            alert.setUpdatedAt(LocalDateTime.now());
            alertRepository.save(alert);
            log.info(LogMessages.ALERT_DELIVERED, alert.getCheckName(), alert.getAlertUrl());
        });
    }

    private void handleFailure(Long alertId, String errorMessage) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            log.warn(LogMessages.ALERT_DELIVERY_FAILED, alert.getCheckName(), alert.getAlertUrl(), errorMessage);
            int newRetryCount = alert.getRetryCount() + 1;
            alert.setRetryCount(newRetryCount);
            alert.setLastError(errorMessage);
            alert.setUpdatedAt(LocalDateTime.now());

            if (newRetryCount >= alert.getMaxRetries()) {
                alert.setStatus(AlertStatus.FAILED_PERMANENTLY);
                log.error(LogMessages.ALERT_PERMANENTLY_FAILED, alert.getId(), newRetryCount);
            } else {
                long backoffSeconds = BackoffPolicy.calculateBackoffSeconds(newRetryCount);
                alert.setNextRetryAt(LocalDateTime.now().plusSeconds(backoffSeconds));
                log.warn(LogMessages.ALERT_RETRY_SCHEDULED, alert.getId(), backoffSeconds);
            }
            alertRepository.save(alert);
        });
    }

    public void sendAlertAsync(Check check, String eventType, String message) {
        Alert alert = createAlertRecord(check, eventType, message);
        dispatchAsync(alert);
    }
}
