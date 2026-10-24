package com.monitor.heartbeat_monitor.ping.controller;

import com.monitor.heartbeat_monitor.ping.service.PingTxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/ping")
@Slf4j
public class PingController {

    private final PingTxService pingTxService;

    public PingController(PingTxService pingTxService) {
        this.pingTxService = pingTxService;
    }

    @RequestMapping(value = "/{token}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> receivePing(@PathVariable String token) {
        LocalDateTime now = LocalDateTime.now();
        PingTxService.PingResult result = pingTxService.processPing(token, now);

        if (!result.found()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invalid ping token.");
        }

        if (result.paused()) {
            return ResponseEntity.ok("Ping recorded. Check is PAUSED; sweeper and alert evaluation suspended.");
        }

        return ResponseEntity.ok("Ping recorded successfully. Next expected at: " + result.nextExpected());
    }
}
