package com.monitor.heartbeat_monitor.ping.service;

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
import com.monitor.heartbeat_monitor.schedule.ScheduleCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class PingTxService {

    private final CheckRepository checkRepository;
    private final CheckHistoryRepository historyRepository;
    private final ScheduleCalculator scheduleCalculator;
    private final AlertService alertService;

    public record PingResult(boolean found, boolean paused, LocalDateTime nextExpected) {}

    public PingTxService(CheckRepository checkRepository,
                         CheckHistoryRepository historyRepository,
                         ScheduleCalculator scheduleCalculator,
                         AlertService alertService) {
        this.checkRepository = checkRepository;
        this.historyRepository = historyRepository;
        this.scheduleCalculator = scheduleCalculator;
        this.alertService = alertService;
    }

    @Transactional
    public PingResult processPing(String token, LocalDateTime now) {
        Optional<Check> optionalCheck = checkRepository.findBySecretTokenWithLock(token);
        if (optionalCheck.isEmpty()) {
            return new PingResult(false, false, null);
        }

        Check check = optionalCheck.get();


        if (CheckStatus.PAUSED.equals(check.getStatus())) {
            check.setLastSeenAt(now);
            check.setUpdatedAt(now);
            checkRepository.save(check);
            return new PingResult(true, true, check.getNextExpectedAt());
        }

        CheckStatus previousStatus = check.getStatus();
        final int CRON_EARLY_WINDOW_MINUTES = 5;
        LocalDateTime baseForNextCalc;
        if ("cron".equalsIgnoreCase(check.getScheduleType())
                && check.getNextExpectedAt() != null
                && check.getNextExpectedAt().isAfter(now)
                && check.getNextExpectedAt().isBefore(now.plusMinutes(CRON_EARLY_WINDOW_MINUTES))) {
            baseForNextCalc = check.getNextExpectedAt();
        } else {
            baseForNextCalc = now;
        }

        LocalDateTime nextExpected = scheduleCalculator.calculateNextExpectedAt(
                check.getScheduleType(),
                check.getScheduleValue(),
                baseForNextCalc
        );

        check.setStatus(CheckStatus.UP);
        check.setLastSeenAt(now);
        check.setNextExpectedAt(nextExpected);
        check.setUpdatedAt(now);
        checkRepository.save(check);


        historyRepository.save(CheckHistory.builder()
                .checkId(check.getId())
                .status(CheckStatus.UP.name())
                .eventType(HistoryEventType.PING)
                .message(String.format(HistoryMessages.PING_RECEIVED, nextExpected))
                .createdAt(now)
                .build());

        if (CheckStatus.DOWN.equals(previousStatus)) {
            Alert alert = alertService.createAlertRecord(
                    check,
                    CheckStatus.UP.name(),
                    String.format(AlertNotificationMessages.HEARTBEAT_RECOVERED, check.getName())
            );
            alertService.dispatchAsync(alert);
            log.info(LogMessages.CHECK_RECOVERED_PING, check.getName());
        }

        return new PingResult(true, false, nextExpected);
    }
}
