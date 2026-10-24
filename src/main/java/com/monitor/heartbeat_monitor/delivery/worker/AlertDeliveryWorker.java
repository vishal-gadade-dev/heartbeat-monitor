package com.monitor.heartbeat_monitor.delivery.worker;

import com.monitor.heartbeat_monitor.alert.enums.AlertStatus;
import com.monitor.heartbeat_monitor.alert.model.Alert;
import com.monitor.heartbeat_monitor.alert.repository.AlertRepository;
import com.monitor.heartbeat_monitor.alert.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class AlertDeliveryWorker {

    private final AlertRepository alertRepository;
    private final AlertService alertService;

    public AlertDeliveryWorker(AlertRepository alertRepository, AlertService alertService) {
        this.alertRepository = alertRepository;
        this.alertService = alertService;
    }


    @Scheduled(fixedDelay = 15000)
    public void retryPendingAlerts() {
        LocalDateTime now = LocalDateTime.now();
        List<Alert> pending = alertRepository.findByStatusAndNextRetryAtLessThanEqual(
                AlertStatus.PENDING,
                now,
                PageRequest.of(0, 50)
        );

        for (Alert alert : pending) {
            alert.setNextRetryAt(now.plusSeconds(30));
            alertRepository.save(alert);

            alertService.dispatch(alert);
        }
    }
}
