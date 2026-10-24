package com.monitor.heartbeat_monitor.history.model;

import com.monitor.heartbeat_monitor.history.enums.HistoryEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "check_history", indexes = {
    @Index(name = "idx_history_check_created", columnList = "checkId, createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long checkId;

    @Column(nullable = false)
    private String status;

    @Enumerated(EnumType.STRING)
    private HistoryEventType eventType;

    private Integer httpStatusCode;

    private Long responseTimeMs;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
