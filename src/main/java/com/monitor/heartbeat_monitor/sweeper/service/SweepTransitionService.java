package com.monitor.heartbeat_monitor.sweeper.service;

import com.monitor.heartbeat_monitor.alert.constant.AlertNotificationMessages;
import com.monitor.heartbeat_monitor.alert.model.Alert;
import com.monitor.heartbeat_monitor.alert.service.AlertService;
import com.monitor.heartbeat_monitor.check.enums.CheckStatus;
import com.monitor.heartbeat_monitor.check.model.Check;
import com.monitor.heartbeat_monitor.check.repository.CheckRepository;
import com.monitor.heartbeat_monitor.common.constant.LogMessages;
import com.monitor.heartbeat_monitor.history.constant.HistoryMessages;
import com.monitor.heartbeat_monitor.history.enums.HistoryEventType;
import com.monitor.heartbeat_monitor.history.model.CheckHistory;
import com.monitor.heartbeat_monitor.history.repository.CheckHistoryRepository;
import com.monitor.heartbeat_monitor.webcheck.dto.ProbeResult;
import com.monitor.heartbeat_monitor.webcheck.service.WebCheckExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class SweepTransitionService {

    private final CheckRepository checkRepository;
    private final CheckHistoryRepository historyRepository;
    private final AlertService alertService;
    private final WebCheckExecutor webCheckExecutor;
    @Autowired
    @Lazy
    private SweepTransitionService self;

    public SweepTransitionService(CheckRepository checkRepository,
                                  CheckHistoryRepository historyRepository,
                                  AlertService alertService,
                                  WebCheckExecutor webCheckExecutor) {
        this.checkRepository = checkRepository;
        this.historyRepository = historyRepository;
        this.alertService = alertService;
        this.webCheckExecutor = webCheckExecutor;
    }

    @Transactional
    public void processHeartbeatCheck(Check check, LocalDateTime now) {
        Optional<Check> opt = checkRepository.findByIdWithLock(check.getId());
        if (opt.isEmpty()) return;
        Check current = opt.get();

        if (current.getNextExpectedAt() == null || CheckStatus.PAUSED.equals(current.getStatus())) {
            return;
        }

        LocalDateTime graceDeadline = current.getNextExpectedAt().plusSeconds(current.getGracePeriodSeconds());

        if (now.isAfter(current.getNextExpectedAt()) && now.isBefore(graceDeadline)) {
            if (!CheckStatus.LATE.equals(current.getStatus()) && !CheckStatus.DOWN.equals(current.getStatus())) {
                current.setStatus(CheckStatus.LATE);
                current.setUpdatedAt(now);
                checkRepository.save(current);

                historyRepository.save(CheckHistory.builder()
                        .checkId(current.getId())
                        .status(CheckStatus.LATE.name())
                        .eventType(HistoryEventType.STATUS_CHANGE)
                        .message(String.format(HistoryMessages.MISSED_DEADLINE_GRACE, graceDeadline))
                        .createdAt(now)
                        .build());
                log.info(LogMessages.CHECK_MOVED_TO_LATE, current.getName());
            }
        } else if (now.isAfter(graceDeadline)) {
            if (!CheckStatus.DOWN.equals(current.getStatus())) {
                current.setStatus(CheckStatus.DOWN);
                current.setUpdatedAt(now);
                checkRepository.save(current);

                historyRepository.save(CheckHistory.builder()
                        .checkId(current.getId())
                        .status(CheckStatus.DOWN.name())
                        .eventType(HistoryEventType.STATUS_CHANGE)
                        .message(HistoryMessages.GRACE_PERIOD_EXPIRED_DOWN)
                        .createdAt(now)
                        .build());

                Alert alert = alertService.createAlertRecord(
                        current,
                        CheckStatus.DOWN.name(),
                        String.format(AlertNotificationMessages.HEARTBEAT_DOWN, current.getName())
                );
                alertService.dispatchAsync(alert);
                log.warn(LogMessages.CHECK_MOVED_TO_DOWN, current.getName());
            }
        }
    }

    public void probeAndHandle(Long checkId, String targetUrl, int intervalSeconds) {
        webCheckExecutor.probeAsync(targetUrl)
                .thenAccept(result -> {
                    LocalDateTime finishedAt = LocalDateTime.now();
                    self.handleWebsiteProbeResult(checkId, result, finishedAt, intervalSeconds);
                })
                .exceptionally(ex -> {
                    log.error("Website probe failed for check id [{}]: {}", checkId, ex.getMessage(), ex);
                    return null;
                });
    }

    @Transactional
    public void handleWebsiteProbeResult(Long checkId, ProbeResult result, LocalDateTime finishedAt, int intervalSeconds) {
        Optional<Check> opt = checkRepository.findByIdWithLock(checkId);
        if (opt.isEmpty()) return;
        Check check = opt.get();

        if (CheckStatus.PAUSED.equals(check.getStatus())) {
            return;
        }

        CheckStatus previousStatus = check.getStatus();

        if (result.isUp()) {
            check.setStatus(CheckStatus.UP);
            check.setLastSeenAt(finishedAt);
            check.setNextExpectedAt(finishedAt.plusSeconds(intervalSeconds));
            check.setUpdatedAt(finishedAt);
            checkRepository.save(check);

            historyRepository.save(CheckHistory.builder()
                    .checkId(check.getId())
                    .status(CheckStatus.UP.name())
                    .eventType(previousStatus.equals(CheckStatus.UP)
                            ? HistoryEventType.WEBSITE_PROBE
                            : HistoryEventType.STATUS_CHANGE)
                    .httpStatusCode(result.getStatusCode())
                    .responseTimeMs(result.getResponseTimeMs())
                    .message(result.getMessage())
                    .createdAt(finishedAt)
                    .build());

            if (CheckStatus.DOWN.equals(previousStatus)) {
                Alert alert = alertService.createAlertRecord(
                        check,
                        CheckStatus.UP.name(),
                        String.format(AlertNotificationMessages.WEBSITE_RECOVERED, check.getName(), result.getStatusCode())
                );
                alertService.dispatchAsync(alert);
                log.info(LogMessages.WEBSITE_RECOVERED, check.getName());
            }
        } else {
            boolean wasDown = CheckStatus.DOWN.equals(previousStatus);
            check.setStatus(CheckStatus.DOWN);
            check.setNextExpectedAt(finishedAt.plusSeconds(intervalSeconds));
            check.setUpdatedAt(finishedAt);
            checkRepository.save(check);

            historyRepository.save(CheckHistory.builder()
                    .checkId(check.getId())
                    .status(CheckStatus.DOWN.name())
                    .eventType(wasDown ? HistoryEventType.WEBSITE_PROBE : HistoryEventType.STATUS_CHANGE)
                    .httpStatusCode(result.getStatusCode())
                    .responseTimeMs(result.getResponseTimeMs())
                    .message(result.getMessage())
                    .createdAt(finishedAt)
                    .build());

            if (!wasDown) {
                Alert alert = alertService.createAlertRecord(
                        check,
                        CheckStatus.DOWN.name(),
                        String.format(AlertNotificationMessages.WEBSITE_DOWN, check.getName(), result.getMessage())
                );
                alertService.dispatchAsync(alert);
                log.warn(LogMessages.WEBSITE_DOWN, check.getName());
            }
        }
    }
}
