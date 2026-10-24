package com.monitor.heartbeat_monitor.check.service;

import com.monitor.heartbeat_monitor.check.dto.CheckDetailsResponse;
import com.monitor.heartbeat_monitor.check.dto.CreateCheckRequest;
import com.monitor.heartbeat_monitor.check.dto.CreateCheckResponse;
import com.monitor.heartbeat_monitor.check.enums.CheckKind;
import com.monitor.heartbeat_monitor.check.enums.CheckStatus;
import com.monitor.heartbeat_monitor.check.model.Check;
import com.monitor.heartbeat_monitor.check.repository.CheckRepository;
import com.monitor.heartbeat_monitor.history.constant.HistoryMessages;
import com.monitor.heartbeat_monitor.history.enums.HistoryEventType;
import com.monitor.heartbeat_monitor.history.model.CheckHistory;
import com.monitor.heartbeat_monitor.history.repository.CheckHistoryRepository;
import com.monitor.heartbeat_monitor.schedule.ScheduleCalculator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class CheckService {

    private final CheckRepository checkRepository;
    private final CheckHistoryRepository historyRepository;
    private final ScheduleCalculator scheduleCalculator;

    public CheckService(CheckRepository checkRepository,
                        CheckHistoryRepository historyRepository,
                        ScheduleCalculator scheduleCalculator) {
        this.checkRepository = checkRepository;
        this.historyRepository = historyRepository;
        this.scheduleCalculator = scheduleCalculator;
    }

    @Transactional
    public CreateCheckResponse createCheck(CreateCheckRequest request, String baseUrl) {
        CheckKind kind = CheckKind.fromString(request.getKind());
        if (kind == null) {
            throw new IllegalArgumentException("Invalid kind: must be 'heartbeat' or 'website'.");
        }

        LocalDateTime now = LocalDateTime.now();
        int gracePeriod = (request.getGracePeriodSeconds() != null)
                ? request.getGracePeriodSeconds() : 300;

        Check check = Check.builder()
                .name(request.getName())
                .kind(kind)
                .alertUrl(request.getAlertUrl())
                .gracePeriodSeconds(gracePeriod)
                .status(CheckStatus.UP)
                .createdAt(now)
                .updatedAt(now)
                .build();

        if (CheckKind.HEARTBEAT.equals(kind)) {
            String scheduleType = request.getScheduleType() != null
                    ? request.getScheduleType().trim().toLowerCase() : "interval";
            String scheduleValue = request.getScheduleValue() != null
                    ? request.getScheduleValue().trim() : "60";

            check.setSecretToken(UUID.randomUUID().toString());
            check.setScheduleType(scheduleType);
            check.setScheduleValue(scheduleValue);

            LocalDateTime initialExpected = scheduleCalculator.calculateNextExpectedAt(scheduleType, scheduleValue, now);
            check.setNextExpectedAt(initialExpected);
            check.setLastSeenAt(now);
        } else {
            if (request.getTargetUrl() == null || request.getTargetUrl().trim().isEmpty()) {
                throw new IllegalArgumentException("targetUrl is required for website checks.");
            }
            int interval = (request.getWebsiteIntervalSeconds() != null && request.getWebsiteIntervalSeconds() > 0)
                    ? request.getWebsiteIntervalSeconds() : 300;

            check.setTargetUrl(request.getTargetUrl().trim());
            check.setWebsiteIntervalSeconds(interval);
            check.setNextExpectedAt(now);
        }

        Check savedCheck = checkRepository.save(check);

        historyRepository.save(CheckHistory.builder()
                .checkId(savedCheck.getId())
                .status(CheckStatus.UP.name())
                .eventType(HistoryEventType.REGISTERED)
                .message(String.format(HistoryMessages.CHECK_REGISTERED, savedCheck.getName(), savedCheck.getKind()))
                .createdAt(now)
                .build());

        return CreateCheckResponse.fromEntity(savedCheck, baseUrl);
    }

    public Optional<CheckDetailsResponse> getCheck(Long id) {
        return checkRepository.findById(id).map(CheckDetailsResponse::fromEntity);
    }

    public Page<CheckDetailsResponse> listChecks(CheckStatus checkStatus, Pageable pageable) {
        if (checkStatus != null) {
            return checkRepository.findByStatus(checkStatus, pageable).map(CheckDetailsResponse::fromEntity);
        }
        return checkRepository.findAll(pageable).map(CheckDetailsResponse::fromEntity);
    }

    @Transactional
    public boolean pauseCheck(Long id) {

        Optional<Check> optional = checkRepository.findByIdWithLock(id);
        if (optional.isEmpty()) return false;

        Check check = optional.get();
        LocalDateTime now = LocalDateTime.now();
        check.setStatus(CheckStatus.PAUSED);
        check.setUpdatedAt(now);
        checkRepository.save(check);

        historyRepository.save(CheckHistory.builder()
                .checkId(id)
                .status(CheckStatus.PAUSED.name())
                .eventType(HistoryEventType.PAUSED)
                .message(HistoryMessages.CHECK_PAUSED)
                .createdAt(now)
                .build());
        return true;
    }

    @Transactional
    public boolean resumeCheck(Long id) {

        Optional<Check> optional = checkRepository.findByIdWithLock(id);
        if (optional.isEmpty()) return false;

        Check check = optional.get();
        LocalDateTime now = LocalDateTime.now();

        Page<CheckHistory> previousHistory = historyRepository.findByCheckIdOrderByCreatedAtDesc(id, PageRequest.of(0, 5));
        CheckStatus statusBeforePause = CheckStatus.UP;
        for (CheckHistory h : previousHistory) {
            if (h.getEventType() != HistoryEventType.PAUSED && h.getEventType() != HistoryEventType.RESUMED) {
                CheckStatus s = CheckStatus.fromString(h.getStatus());
                if (s != null) {
                    statusBeforePause = s;
                    break;
                }
            }
        }

        if (CheckStatus.DOWN.equals(statusBeforePause)) {
            check.setStatus(CheckStatus.DOWN);
        } else {
            check.setStatus(CheckStatus.UP);
        }

        if (CheckKind.HEARTBEAT.equals(check.getKind())) {
            LocalDateTime nextExpected = scheduleCalculator.calculateNextExpectedAt(
                    check.getScheduleType(),
                    check.getScheduleValue(),
                    now
            );
            check.setNextExpectedAt(nextExpected);
        } else {
            int interval = check.getWebsiteIntervalSeconds() != null
                    ? check.getWebsiteIntervalSeconds() : 300;
            check.setNextExpectedAt(now.plusSeconds(interval));
        }

        check.setUpdatedAt(now);
        checkRepository.save(check);

        historyRepository.save(CheckHistory.builder()
                .checkId(id)
                .status(check.getStatus().name())
                .eventType(HistoryEventType.RESUMED)
                .message(HistoryMessages.CHECK_RESUMED)
                .createdAt(now)
                .build());
        return true;
    }
}
