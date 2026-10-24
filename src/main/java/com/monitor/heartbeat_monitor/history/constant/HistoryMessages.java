package com.monitor.heartbeat_monitor.history.constant;

public final class HistoryMessages {
    public static final String CHECK_REGISTERED = "Check registered: %s (%s)";
    public static final String CHECK_PAUSED = "Check paused. Sweeper and alerts suspended.";
    public static final String CHECK_RESUMED = "Check resumed. Next expected deadline reset starting from now.";
    public static final String PING_RECEIVED = "Heartbeat ping received from client. Next expected at: %s";
    public static final String MISSED_DEADLINE_GRACE = "Missed expected check-in deadline; currently in grace period until %s";
    public static final String GRACE_PERIOD_EXPIRED_DOWN = "Grace period expired without receiving ping. Check is DOWN.";

}
