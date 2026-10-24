package com.monitor.heartbeat_monitor.delivery.controller;

import com.monitor.heartbeat_monitor.alert.enums.AlertStatus;
import com.monitor.heartbeat_monitor.alert.model.Alert;
import com.monitor.heartbeat_monitor.alert.repository.AlertRepository;
import com.monitor.heartbeat_monitor.alert.service.AlertService;
import com.monitor.heartbeat_monitor.delivery.dto.AlertResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/alerts")
public class AlertDeliveryController {

    private final AlertRepository alertRepository;
    private final AlertService alertService;

    public AlertDeliveryController(AlertRepository alertRepository, AlertService alertService) {
        this.alertRepository = alertRepository;
        this.alertService = alertService;
    }

    @GetMapping("/failed")
    public ResponseEntity<Page<AlertResponse>> getFailedAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int cappedSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), cappedSize, Sort.by("createdAt").descending());
        Page<Alert> result;

        if (status != null && !status.trim().isEmpty()) {
            if ("all".equalsIgnoreCase(status.trim())) {
                result = alertRepository.findAll(pageable);
            } else {
                AlertStatus alertStatus = AlertStatus.fromString(status);
                result = alertStatus != null
                        ? alertRepository.findByStatus(alertStatus, pageable)
                        : alertRepository.findByStatus(AlertStatus.FAILED_PERMANENTLY, pageable);
            }
        } else {
            result = alertRepository.findByStatus(AlertStatus.FAILED_PERMANENTLY, pageable);
        }

        return ResponseEntity.ok(result.map(AlertResponse::fromEntity));
    }

    @PostMapping("/failed/{id}/retry")
    public ResponseEntity<AlertResponse> retryAlert(@PathVariable Long id) {
        Optional<Alert> opt = alertRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Alert alert = opt.get();
        if (!AlertStatus.FAILED_PERMANENTLY.equals(alert.getStatus())) {
            return ResponseEntity.status(409).body(AlertResponse.fromEntity(alert));
        }

        alert.setStatus(AlertStatus.PENDING);
        alert.setRetryCount(0);
        alert.setLastError(null);
        alert.setNextRetryAt(LocalDateTime.now());
        alert.setUpdatedAt(LocalDateTime.now());
        alertRepository.save(alert);

        alertService.dispatchAsync(alert);
        return ResponseEntity.ok(AlertResponse.fromEntity(alert));
    }
}
