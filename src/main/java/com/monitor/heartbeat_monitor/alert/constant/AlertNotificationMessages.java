package com.monitor.heartbeat_monitor.alert.constant;

public final class AlertNotificationMessages {
    public static final String HEARTBEAT_RECOVERED = "Check [%s] has recovered and received a successful ping.";
    public static final String HEARTBEAT_DOWN = "Heartbeat check [%s] is DOWN (deadline and grace period expired).";
    public static final String WEBSITE_RECOVERED = "Website [%s] has recovered and is UP (HTTP %d).";
    public static final String WEBSITE_DOWN = "Website [%s] is DOWN: %s";
}
