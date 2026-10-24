package com.monitor.heartbeat_monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class HeartbeatMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeartbeatMonitorApplication.class, args);
    }
}
