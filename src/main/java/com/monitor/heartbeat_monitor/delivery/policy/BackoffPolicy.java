package com.monitor.heartbeat_monitor.delivery.policy;

public final class BackoffPolicy {

    private static final int INITIAL_BACKOFF_SECONDS = 300;

    private BackoffPolicy() {}

    public static long calculateBackoffSeconds(int retryCount) {
        return (long) (INITIAL_BACKOFF_SECONDS * Math.pow(3, Math.max(0, retryCount - 1)));
    }
}
