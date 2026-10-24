package com.monitor.heartbeat_monitor.common.config;

public final class ThreadPoolConfig {
    public static final int WEBSITE_PROBE_CORE_POOL_SIZE = 10;
    public static final int WEBSITE_PROBE_MAX_POOL_SIZE  = 50;
    public static final int WEBSITE_PROBE_QUEUE_CAPACITY = 10_000;
    public static final long WEBSITE_PROBE_KEEP_ALIVE_SEC = 60L;

    public static final int ALERT_DISPATCH_CORE_POOL_SIZE = 5;
    public static final int ALERT_DISPATCH_MAX_POOL_SIZE  = 20;
    public static final int ALERT_DISPATCH_QUEUE_CAPACITY = 200;
    public static final long ALERT_DISPATCH_KEEP_ALIVE_SEC = 60L;

    public static final int HTTP_CONNECT_TIMEOUT_SEC = 5;
    public static final int HTTP_READ_TIMEOUT_SEC = 5;

    private ThreadPoolConfig() {}
}
