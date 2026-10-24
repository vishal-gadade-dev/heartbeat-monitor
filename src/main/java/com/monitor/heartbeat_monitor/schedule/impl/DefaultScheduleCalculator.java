package com.monitor.heartbeat_monitor.schedule.impl;

import com.monitor.heartbeat_monitor.schedule.ScheduleCalculator;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class DefaultScheduleCalculator implements ScheduleCalculator {

    private static final String CRON_DELIMITER_REGEX = "\\s+";
    private static final String CRON_ZERO_PREFIX = "0 ";

    @Override
    public LocalDateTime calculateNextExpectedAt(String scheduleType, String scheduleValue, LocalDateTime from) {
        if (scheduleType == null || scheduleValue == null || scheduleValue.trim().isEmpty()) {
            return from.plusMinutes(60);
        }

        String type = scheduleType.trim().toLowerCase();
        String value = scheduleValue.trim();

        if ("interval".equals(type)) {
            try {
                int minutes = Integer.parseInt(value);
                return from.plusMinutes(minutes);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid interval value. Expected integer minutes: " + value, e);
            }
        } else if ("cron".equals(type)) {
            return calculateNextCron(value, from);
        } else {
            throw new IllegalArgumentException("Unsupported scheduleType: " + scheduleType + ". Expected 'interval' or 'cron'.");
        }
    }

    private LocalDateTime calculateNextCron(String cronString, LocalDateTime from) {
        String[] parts = cronString.trim().split(CRON_DELIMITER_REGEX);
        String springCron;
        if (parts.length == 5) {
            springCron = CRON_ZERO_PREFIX + cronString.trim();
        } else if (parts.length == 6) {
            springCron = cronString.trim();
        } else {
            throw new IllegalArgumentException("Invalid cron expression: " + cronString + ". Expected 5 or 6 fields.");
        }

        try {
            CronExpression expression = CronExpression.parse(springCron);
            ZoneId zoneId = ZoneId.systemDefault();
            ZonedDateTime zonedFrom = ZonedDateTime.of(from, zoneId);
            ZonedDateTime next = expression.next(zonedFrom);
            if (next == null) {
                throw new IllegalStateException("Could not calculate next execution time for cron: " + cronString);
            }
            return next.toLocalDateTime();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to parse cron expression: " + cronString + ": " + e.getMessage(), e);
        }
    }
}
