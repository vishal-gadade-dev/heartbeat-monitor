package com.monitor.heartbeat_monitor.webcheck.constant;

public final class ProbeMessages {
    public static final String HTTP_OK = "HTTP %d OK";
    public static final String HTTP_NON_2XX = "HTTP non-2xx response: %d";
    public static final String HTTP_ERROR = "HTTP error: %d";
    public static final String NETWORK_TIMEOUT = "Network timeout or connection refused (exceeded 5s limit): %s";
    public static final String RESPONSE_TIME_EXCEEDED = "Response exceeded 5s limit (took %dms)";
    public static final String QUEUE_FULL_REJECTED = "Website probe worker queue is full (task rejected)";
    public static final String PROBE_FAILED = "Probe failed: %s";

    private ProbeMessages() {}
}
