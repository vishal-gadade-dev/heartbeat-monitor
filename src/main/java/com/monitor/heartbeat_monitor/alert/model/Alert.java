package com.monitor.heartbeat_monitor.alert.model;
import com.monitor.heartbeat_monitor.alert.enums.AlertStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "failed_alerts", indexes = {
    @Index(name = "idx_failed_alerts_status_retry", columnList = "status, nextRetryAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long checkId;

    private String checkName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String alertUrl;

    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Builder.Default
    private int retryCount = 0;

    @Builder.Default
    private int maxRetries = 5;

    private LocalDateTime nextRetryAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AlertStatus status = AlertStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
