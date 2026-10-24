package com.monitor.heartbeat_monitor.alert.enums;

public enum AlertStatus {
    PENDING,
    DELIVERED,
    FAILED_PERMANENTLY;

    public static AlertStatus fromString(String value) {
        if (value == null) return null;
        for (AlertStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        return null;
    }
}
