package com.monitor.heartbeat_monitor.common.constant;

public final class LogMessages {
    // Ping
    public static final String CHECK_RECOVERED_PING = "Check [{}] recovered from DOWN state. Recovery alert sent.";
    public static final String CONCURRENT_PING_RETRY = "Concurrent race detected on check [{}] during ping, retrying attempt [{}]";

    // Alert
    public static final String ALERT_DELIVERED = "Alert delivered successfully for check [{}] to [{}]";
    public static final String ALERT_DELIVERY_FAILED = "Failed to deliver alert for check [{}] to [{}]: {}. Moving to retry queue.";
    public static final String ALERT_PERMANENTLY_FAILED = "Alert id [{}] permanently failed after [{}] retries. Preserved in DLQ.";
    public static final String ALERT_RETRY_SCHEDULED = "Alert id [{}] retry failed. Next retry in {}s";

    // Sweeper
    public static final String ERROR_PROCESSING_DUE_CHECK = "Error processing due check id [{}]: {}";
    public static final String CHECK_MOVED_TO_LATE = "Check [{}] moved to LATE (in grace period).";
    public static final String CONCURRENT_PING_LATE_WIN = "Concurrent ping arrived for check [{}] during LATE transition; ping wins.";
    public static final String CHECK_MOVED_TO_DOWN = "Check [{}] moved to DOWN. Alert dispatched.";
    public static final String CONCURRENT_PING_DOWN_WIN = "Concurrent ping arrived for check [{}] during DOWN transition; ping won race, alert suppressed.";
    public static final String WEBSITE_RECOVERED = "Website [{}] recovered. Recovery alert dispatched.";
    public static final String WEBSITE_DOWN = "Website [{}] went DOWN. Alert dispatched.";

    private LogMessages() {}
}
