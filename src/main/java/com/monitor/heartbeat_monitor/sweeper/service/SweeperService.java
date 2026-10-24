package com.monitor.heartbeat_monitor.sweeper.service;

import com.monitor.heartbeat_monitor.check.enums.CheckKind;
import com.monitor.heartbeat_monitor.check.model.Check;
import com.monitor.heartbeat_monitor.check.repository.CheckRepository;
import com.monitor.heartbeat_monitor.common.constant.LogMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
@Service
@Slf4j
public class SweeperService {

    private final CheckRepository checkRepository;
    private final SweepTransitionService transitionService;

    public SweeperService(CheckRepository checkRepository,
                          SweepTransitionService transitionService) {
        this.checkRepository = checkRepository;
        this.transitionService = transitionService;
    }

    public void sweepChecks() {
        LocalDateTime now = LocalDateTime.now();
        List<Check> dueChecks = checkRepository.findDueChecksForSweep(now);

        for (Check check : dueChecks) {
            try {
                if (CheckKind.HEARTBEAT.equals(check.getKind())) {
                    transitionService.processHeartbeatCheck(check, now);
                } else if (CheckKind.WEBSITE.equals(check.getKind())) {
                    int intervalSeconds = check.getWebsiteIntervalSeconds() != null
                            ? check.getWebsiteIntervalSeconds()
                            : 300;
                    check.setNextExpectedAt(now.plusSeconds(intervalSeconds));
                    checkRepository.save(check);
                    transitionService.probeAndHandle(check.getId(), check.getTargetUrl(), intervalSeconds);
                }
            } catch (Exception e) {
                log.error(LogMessages.ERROR_PROCESSING_DUE_CHECK, check.getId(), e.getMessage(), e);
            }
        }
    }
}
