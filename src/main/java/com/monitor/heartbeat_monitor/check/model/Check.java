package com.monitor.heartbeat_monitor.check.model;
import com.monitor.heartbeat_monitor.check.enums.CheckKind;
import com.monitor.heartbeat_monitor.check.enums.CheckKindConverter;
import com.monitor.heartbeat_monitor.check.enums.CheckStatus;
import com.monitor.heartbeat_monitor.check.enums.CheckStatusConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "checks", indexes = {
    @Index(name = "idx_checks_kind_status_next_expected", columnList = "kind, status, nextExpectedAt"),
    @Index(name = "idx_checks_secret_token", columnList = "secretToken")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Check {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Convert(converter = CheckKindConverter.class)
    @Column(nullable = false)
    private CheckKind kind;

    @Column(unique = true)
    private String secretToken;

    private String scheduleType;
    private String scheduleValue;

    @Column(columnDefinition = "TEXT")
    private String targetUrl;

    @Builder.Default
    private Integer websiteIntervalSeconds = 300;

    @Builder.Default
    private Integer gracePeriodSeconds = 300;

    @Convert(converter = CheckStatusConverter.class)
    @Builder.Default
    private CheckStatus status = CheckStatus.UP;

    private LocalDateTime lastSeenAt;
    private LocalDateTime nextExpectedAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String alertUrl;

    @Version
    private Long version;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
