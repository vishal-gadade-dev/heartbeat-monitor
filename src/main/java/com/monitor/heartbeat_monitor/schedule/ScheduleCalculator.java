package com.monitor.heartbeat_monitor.schedule;

import java.time.LocalDateTime;

public interface ScheduleCalculator {

    LocalDateTime calculateNextExpectedAt(String scheduleType, String scheduleValue, LocalDateTime from);
}
