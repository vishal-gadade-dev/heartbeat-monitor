package com.monitor.heartbeat_monitor.schedule;

import com.monitor.heartbeat_monitor.schedule.ScheduleCalculator;
import com.monitor.heartbeat_monitor.schedule.impl.DefaultScheduleCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleCalculatorTest {

    private ScheduleCalculator scheduleCalculator;

    @BeforeEach
    void setUp() {
        scheduleCalculator = new DefaultScheduleCalculator();
    }

    @Test
    void testIntervalSchedule_AddsMinutes() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 23, 10, 0);
        LocalDateTime next = scheduleCalculator.calculateNextExpectedAt("interval", "60", now);
        assertEquals(LocalDateTime.of(2026, 10, 23, 11, 0), next);
    }

    @Test
    void testCronSchedule_5Field_DailyAtThreeAM() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 23, 2, 0);
        LocalDateTime next = scheduleCalculator.calculateNextExpectedAt("cron", "0 3 * * *", from);
        assertEquals(LocalDateTime.of(2026, 10, 23, 3, 0), next);
    }

    @Test
    void testCronSchedule_WeekdaysOnly_SkipsWeekend() {
        // Friday 2026-10-23 10:00 AM -> Next weekday at 09:00 AM is Monday 2026-10-26 09:00 AM
        LocalDateTime friday = LocalDateTime.of(2026, 10, 23, 10, 0);
        LocalDateTime next = scheduleCalculator.calculateNextExpectedAt("cron", "0 9 * * 1-5", friday);
        assertEquals(LocalDateTime.of(2026, 10, 26, 9, 0), next);
    }

    @Test
    void testInvalidCron_ThrowsException() {
        LocalDateTime now = LocalDateTime.now();
        assertThrows(IllegalArgumentException.class, () ->
                scheduleCalculator.calculateNextExpectedAt("cron", "invalid cron string", now));
    }
}
