package com.monitor.heartbeat_monitor.webcheck.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProbeResult {
    private boolean up;
    private Integer statusCode;
    private long responseTimeMs;
    private String message;
}
