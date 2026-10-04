package com.hchen.appretention.hook.system.opt;

import org.junit.Test;
import static org.junit.Assert.*;

public class FcmHeartbeatPolicyTest {
    @Test public void defaultOnAndFiveMinuteInterval() {
        assertTrue(FcmHeartbeatPolicy.DEFAULT_ENABLED);
        assertEquals(300000L, FcmHeartbeatPolicy.INTERVAL_MS);
    }
    @Test public void onRequestsAndOffDoesNot() {
        FcmHeartbeatPolicy p = new FcmHeartbeatPolicy();
        assertTrue(p.shouldRequest(true, true, true, false, 0));
        assertFalse(p.shouldRequest(false, true, true, false, 0));
    }
    @Test public void skipsOfflineUnavailableAndRestricted() {
        FcmHeartbeatPolicy p = new FcmHeartbeatPolicy();
        assertFalse(p.shouldRequest(true, false, true, false, 0));
        assertFalse(p.shouldRequest(true, true, false, false, 0));
        assertFalse(p.shouldRequest(true, true, true, true, 0));
        assertTrue(p.shouldRequest(true, true, true, false, 0));
    }
    @Test public void rateLimitHasExactBoundary() {
        FcmHeartbeatPolicy p = new FcmHeartbeatPolicy();
        p.markRequested(1000);
        assertFalse(p.shouldRequest(true, true, true, false, 300999));
        assertTrue(p.shouldRequest(true, true, true, false, 301000));
    }
    @Test public void toggleDoesNotBypassRateLimit() {
        FcmHeartbeatPolicy p = new FcmHeartbeatPolicy();
        p.markRequested(1000);
        assertFalse(p.shouldRequest(false, true, true, false, 2000));
        assertFalse(p.shouldRequest(true, true, true, false, 3000));
        assertTrue(p.shouldRequest(true, true, true, false, 301000));
    }
    @Test public void rejectedChecksDoNotConsumeNextRequest() {
        FcmHeartbeatPolicy p = new FcmHeartbeatPolicy();
        assertFalse(p.shouldRequest(true, false, true, false, 1000));
        assertTrue(p.shouldRequest(true, true, true, false, 1001));
    }
}
