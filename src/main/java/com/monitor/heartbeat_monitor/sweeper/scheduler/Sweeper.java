package com.monitor.heartbeat_monitor.sweeper.scheduler;

import com.monitor.heartbeat_monitor.sweeper.service.SweeperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Sweeper {

    private final SweeperService sweeperService;

    public Sweeper(SweeperService sweeperService) {
        this.sweeperService = sweeperService;
    }

    @Scheduled(fixedDelay = 10000)
    public void runSweep() {
        sweeperService.sweepChecks();
    }
}
