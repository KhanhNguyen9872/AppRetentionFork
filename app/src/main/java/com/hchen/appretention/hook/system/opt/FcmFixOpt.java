package com.hchen.appretention.hook.system.opt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.hchen.appretention.log.XposedLog;
import com.hchen.collect.HookEntrance;
import com.hchen.hooktool.HCBase;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.utils.SystemPropTool;

import java.lang.reflect.Method;

/**
 * Owner-user, system_server heartbeat assistance; no GMS internals or token changes.
 * MCS_HEARTBEAT is an undocumented, best-effort GMS action. Receiving it and
 * reconnecting are GMS decisions, not a success condition this module can observe.
 */
@HookEntrance(targetPackage = "android", targetSdks = 23, upward = true)
public final class FcmFixOpt extends HCBase {
    public static final String ACTION_CONFIG_CHANGED = "com.hchen.appretention.FCM_CONFIG_CHANGED";
    private static final String ACTION_TICK = "com.hchen.appretention.FCM_HEARTBEAT_TICK";
    private static final String ACTION_HEARTBEAT = "com.google.android.intent.action.MCS_HEARTBEAT";
    private static final String GMS = "com.google.android.gms";
    private static FcmFixOpt running;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FcmHeartbeatPolicy policy = new FcmHeartbeatPolicy();
    private Context context;
    private AlarmManager alarms;
    private PendingIntent tickIntent;

    @Override
    protected void init() {
        Class<?> ams = findClassIfExists("com.android.server.am.ActivityManagerService");
        if (ams == null) {
            XposedLog.logW(TAG, "AMS unavailable; FCM assistance not installed.");
            return;
        }
        // Install even when OFF so a successful UI/property change can enable it live.
        for (Method method : ams.getDeclaredMethods()) {
            if (!"finishBooting".equals(method.getName())) continue;
            hook(method, new IHook() {
                @Override
                public void after() {
                    try {
                        Context systemContext = (Context) getThisField("mContext");
                        handler.post(() -> start(systemContext));
                    } catch (Throwable error) {
                        XposedLog.logW(TAG, "Could not obtain system context for FCM assistance.", error);
                    }
                }
            });
            XposedLog.logI(TAG, "FCM assistance boot hook installed (owner user).");
            return;
        }
        XposedLog.logW(TAG, "AMS.finishBooting unavailable; FCM assistance not installed.");
    }

    private void start(Context systemContext) {
        if (running != null || systemContext == null) return;
        context = systemContext;
        BroadcastReceiver tickReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ignored, Intent intent) { tick(); }
        };
        BroadcastReceiver configReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ignored, Intent intent) {
                // Do not trust intent extras; read only the authoritative property.
                tick();
            }
        };
        boolean tickRegistered = false;
        try {
            alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarms == null) throw new IllegalStateException("AlarmManager unavailable");
            tickIntent = PendingIntent.getBroadcast(context, 0xFC01,
                new Intent(ACTION_TICK).setPackage("android"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(tickReceiver, new IntentFilter(ACTION_TICK),
                    null, handler, Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(tickReceiver, new IntentFilter(ACTION_TICK),
                    "android.permission.DUMP", handler);
            }
            tickRegistered = true;
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(configReceiver, new IntentFilter(ACTION_CONFIG_CHANGED),
                    "android.permission.DUMP", handler, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(configReceiver, new IntentFilter(ACTION_CONFIG_CHANGED),
                    "android.permission.DUMP", handler);
            }
            running = this;
            tick();
        } catch (Throwable error) {
            if (tickRegistered) {
                try { context.unregisterReceiver(tickReceiver); } catch (Throwable ignored) { }
            }
            XposedLog.logW(TAG, "FCM scheduler initialization failed; other hooks are unchanged.", error);
        }
    }

    private boolean enabled() {
        return SystemPropTool.getProp(FcmHeartbeatPolicy.PROP_ENABLED,
            FcmHeartbeatPolicy.DEFAULT_ENABLED);
    }

    private void tick() {
        try {
            alarms.cancel(tickIntent);
            if (!enabled()) {
                XposedLog.logD(TAG, "Fix FCM OFF; heartbeat alarm cancelled.");
                return;
            }
            long now = SystemClock.elapsedRealtime();
            boolean available = false;
            try {
                ApplicationInfo app = context.getPackageManager().getApplicationInfo(GMS, 0);
                // Respect an explicit force-stop; do not clear stopped state or wake it.
                available = app.enabled && (app.flags & ApplicationInfo.FLAG_STOPPED) == 0;
            } catch (android.content.pm.PackageManager.NameNotFoundException ignored) { }
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = cm == null ? null : cm.getActiveNetwork();
            NetworkCapabilities caps = network == null ? null : cm.getNetworkCapabilities(network);
            boolean online = caps != null
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            if (policy.shouldRequest(enabled(), online, available,
                    BackgroundRestrictOpt.isRestricted(GMS), now)) {
                context.sendBroadcast(new Intent(ACTION_HEARTBEAT).setPackage(GMS));
                policy.markRequested(now);
                XposedLog.logD(TAG, "MCS heartbeat requested; FCM connection state unverified.");
            }
        } catch (Throwable error) {
            XposedLog.logW(TAG, "FCM heartbeat request failed (best effort).", error);
        } finally {
            try {
                if (enabled()) {
                    // Inexact and idle-aware: no exact-alarm permission, no permanent wakelock.
                    alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + FcmHeartbeatPolicy.INTERVAL_MS, tickIntent);
                }
            } catch (Throwable error) {
                XposedLog.logW(TAG, "FCM alarm scheduling failed.", error);
            }
        }
    }
}
