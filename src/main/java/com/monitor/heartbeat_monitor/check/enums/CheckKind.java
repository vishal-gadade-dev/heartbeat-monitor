package com.monitor.heartbeat_monitor.check.enums;

public enum CheckKind {
    HEARTBEAT,
    WEBSITE;

    public static CheckKind fromString(String value) {
        if (value == null) return null;
        for (CheckKind kind : values()) {
            if (kind.name().equalsIgnoreCase(value.trim())) {
                return kind;
            }
        }
        return null;
    }
}
