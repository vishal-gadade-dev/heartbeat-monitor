package com.monitor.heartbeat_monitor.check.dto;

import com.monitor.heartbeat_monitor.check.enums.CheckKind;
import com.monitor.heartbeat_monitor.check.enums.CheckStatus;
import com.monitor.heartbeat_monitor.check.model.Check;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckDetailsResponse {

    private Long id;
    private String name;
    private CheckKind kind;
    private CheckStatus status;
    private String scheduleType;
    private String scheduleValue;
    private String targetUrl;
    private Integer websiteIntervalSeconds;
    private Integer gracePeriodSeconds;
    private LocalDateTime lastSeenAt;
    private LocalDateTime nextExpectedAt;
    private String alertUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CheckDetailsResponse fromEntity(Check check) {
        return CheckDetailsResponse.builder()
                .id(check.getId())
                .name(check.getName())
                .kind(check.getKind())
                .status(check.getStatus())
                .scheduleType(check.getScheduleType())
                .scheduleValue(check.getScheduleValue())
                .targetUrl(check.getTargetUrl())
                .websiteIntervalSeconds(check.getWebsiteIntervalSeconds())
                .gracePeriodSeconds(check.getGracePeriodSeconds())
                .lastSeenAt(check.getLastSeenAt())
                .nextExpectedAt(check.getNextExpectedAt())
                .alertUrl(check.getAlertUrl())
                .createdAt(check.getCreatedAt())
                .updatedAt(check.getUpdatedAt())
                .build();
    }
}
