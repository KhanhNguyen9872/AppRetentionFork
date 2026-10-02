package com.hchen.appretention.hook.system.opt;

/** Monotonic rate limit; a dispatch is NOT proof of a connected FCM socket. */
public final class FcmHeartbeatPolicy {
    public static final String PROP_ENABLED = "persist.hchen.fcm.fix.enable";
    public static final boolean DEFAULT_ENABLED = true;
    public static final long INTERVAL_MS = 5 * 60 * 1000L;
    private long lastRequest = -1;

    public boolean shouldRequest(boolean enabled, boolean online, boolean gmsAvailable,
                                 boolean restricted, long elapsedRealtime) {
        return enabled && online && gmsAvailable && !restricted
            && (lastRequest < 0 || elapsedRealtime - lastRequest >= INTERVAL_MS);
    }

    public void markRequested(long elapsedRealtime) {
        lastRequest = elapsedRealtime;
    }
}
