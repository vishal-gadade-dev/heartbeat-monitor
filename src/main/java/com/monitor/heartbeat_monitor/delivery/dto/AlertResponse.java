package com.monitor.heartbeat_monitor.delivery.dto;

import com.monitor.heartbeat_monitor.alert.enums.AlertStatus;
import com.monitor.heartbeat_monitor.alert.model.Alert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private Long id;
    private Long checkId;
    private String checkName;
    private String alertUrl;
    private String eventType;
    private String payload;
    private int retryCount;
    private int maxRetries;
    private LocalDateTime nextRetryAt;
    private AlertStatus status;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AlertResponse fromEntity(Alert alert) {
        if (alert == null) return null;
        return AlertResponse.builder()
                .id(alert.getId())
                .checkId(alert.getCheckId())
                .checkName(alert.getCheckName())
                .alertUrl(alert.getAlertUrl())
                .eventType(alert.getEventType())
                .payload(alert.getPayload())
                .retryCount(alert.getRetryCount())
                .maxRetries(alert.getMaxRetries())
                .nextRetryAt(alert.getNextRetryAt())
                .status(alert.getStatus())
                .lastError(alert.getLastError())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }
}
