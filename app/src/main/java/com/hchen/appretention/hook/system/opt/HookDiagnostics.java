package com.hchen.appretention.hook.system.opt;

import com.hchen.appretention.log.XposedLog;
import com.hchen.hooktool.core.CoreTool;
import com.hchen.hooktool.hook.IHook;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Counts completed registrations, not attempted hooks or observed protection. */
public final class HookDiagnostics {
    private static final ConcurrentHashMap<String, AtomicInteger> COUNTS = new ConcurrentHashMap<>();
    private HookDiagnostics() {}
    public static boolean install(String feature, Method method, IHook callback) {
        try {
            if (CoreTool.hook(method, callback) == null) return false;
            COUNTS.computeIfAbsent(feature, key -> new AtomicInteger()).incrementAndGet();
            return true;
        } catch (Throwable error) {
            XposedLog.logENoSave(feature, "Registration failed: " + method, error);
            return false;
        }
    }
    public static int count(String feature) {
        AtomicInteger value = COUNTS.get(feature);
        return value == null ? 0 : value.get();
    }
    public static int total() {
        int total = 0;
        for (AtomicInteger value : COUNTS.values()) total += value.get();
        return total;
    }
    public static void report(String feature) {
        int count = count(feature);
        if (count == 0) XposedLog.logW(feature, "No compatible hooks registered; feature unavailable on this ROM.");
        else XposedLog.logINoSave(feature, "Registered " + count + " hooks; effects depend on the live setting and target workload.");
    }
}
