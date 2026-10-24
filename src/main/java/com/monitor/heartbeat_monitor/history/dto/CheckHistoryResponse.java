package com.monitor.heartbeat_monitor.history.dto;

import com.monitor.heartbeat_monitor.history.enums.HistoryEventType;
import com.monitor.heartbeat_monitor.history.model.CheckHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckHistoryResponse {
    private Long id;
    private Long checkId;
    private String status;
    private HistoryEventType eventType;
    private Integer httpStatusCode;
    private Long responseTimeMs;
    private String message;
    private LocalDateTime createdAt;

    public static CheckHistoryResponse fromEntity(CheckHistory history) {
        if (history == null) return null;
        return CheckHistoryResponse.builder()
                .id(history.getId())
                .checkId(history.getCheckId())
                .status(history.getStatus())
                .eventType(history.getEventType())
                .httpStatusCode(history.getHttpStatusCode())
                .responseTimeMs(history.getResponseTimeMs())
                .message(history.getMessage())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
