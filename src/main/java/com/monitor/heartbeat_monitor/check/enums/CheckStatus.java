package com.monitor.heartbeat_monitor.check.enums;

public enum CheckStatus {
    UP,
    LATE,
    DOWN,
    PAUSED;

    public static CheckStatus fromString(String value) {
        if (value == null) return null;
        for (CheckStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return null;
    }
}
