package com.monitor.heartbeat_monitor.check.controller;

import com.monitor.heartbeat_monitor.check.dto.CheckDetailsResponse;
import com.monitor.heartbeat_monitor.check.dto.CreateCheckRequest;
import com.monitor.heartbeat_monitor.check.dto.CreateCheckResponse;
import com.monitor.heartbeat_monitor.check.enums.CheckStatus;
import com.monitor.heartbeat_monitor.check.service.CheckService;
import com.monitor.heartbeat_monitor.history.dto.CheckHistoryResponse;
import com.monitor.heartbeat_monitor.history.repository.CheckHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Optional;

@RestController
@RequestMapping("/checks")
public class CheckController {

    private final CheckService checkService;
    private final CheckHistoryRepository historyRepository;

    public CheckController(CheckService checkService, CheckHistoryRepository historyRepository) {
        this.checkService = checkService;
        this.historyRepository = historyRepository;
    }

    @PostMapping
    public ResponseEntity<CreateCheckResponse> registerCheck(@Valid @RequestBody CreateCheckRequest request, HttpServletRequest httpRequest) {
        String baseUrl = ServletUriComponentsBuilder.fromContextPath(httpRequest).build().toUriString();
        CreateCheckResponse response = checkService.createCheck(request, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CheckDetailsResponse> getCheckStatus(@PathVariable Long id) {
        Optional<CheckDetailsResponse> optional = checkService.getCheck(id);
        return optional.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<CheckDetailsResponse>> listChecks(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        CheckStatus checkStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            checkStatus = CheckStatus.fromString(status);
            if (checkStatus == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: '" + status + "'. Allowed values: UP, LATE, DOWN, PAUSED.");
            }
        }

        int cappedSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), cappedSize, Sort.by("id").descending());
        return ResponseEntity.ok(checkService.listChecks(checkStatus, pageable));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<Page<CheckHistoryResponse>> getCheckHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        int cappedSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(Math.max(0, page), cappedSize, Sort.by("createdAt").descending());
        Page<CheckHistoryResponse> historyPage = historyRepository.findByCheckIdOrderByCreatedAtDesc(id, pageable)
                .map(CheckHistoryResponse::fromEntity);
        return ResponseEntity.ok(historyPage);
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<?> pauseCheck(@PathVariable Long id) {
        boolean success = checkService.pauseCheck(id);
        if (!success) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Check paused successfully.");
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<?> resumeCheck(@PathVariable Long id) {
        boolean success = checkService.resumeCheck(id);
        if (!success) return ResponseEntity.notFound().build();
        return ResponseEntity.ok("Check resumed successfully.");
    }
}
