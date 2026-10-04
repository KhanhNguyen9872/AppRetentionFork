package com.hchen.appretention.hook.system.opt;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import com.hchen.appretention.BuildConfig;
import com.hchen.appretention.log.XposedLog;
import com.hchen.collect.HookEntrance;
import com.hchen.hooktool.HCBase;
import com.hchen.hooktool.hook.IHook;
import java.lang.reflect.Method;

/** Root-requested, nonce-bound status; registration is not proof of runtime effects. */
@HookEntrance(targetPackage = "android")
public final class HookStatus extends HCBase {
    public static final String REQUEST = "com.hchen.appretention.HOOK_STATUS_REQUEST";
    public static final String RESPONSE = "com.hchen.appretention.HOOK_STATUS_RESPONSE";
    private static boolean registered;
    @Override protected void init() {
        Class<?> ams = findClassIfExists("com.android.server.am.ActivityManagerService");
        if (ams == null) { HookDiagnostics.report(TAG); return; }
        for (Method method : ams.getDeclaredMethods()) {
            if (!"finishBooting".equals(method.getName())) continue;
            if (!HookDiagnostics.install(TAG, method, new IHook() {
                @Override public void after() {
                    try { register((Context) getThisField("mContext")); }
                    catch (Throwable error) { XposedLog.logENoSave(TAG, "Status receiver unavailable", error); }
                }
            })) continue;
            break;
        }
        HookDiagnostics.report(TAG);
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag") // Explicit flags on Android 13+.
    private static synchronized void register(Context context) {
        if (registered || context == null) return;
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ignored, Intent request) {
                if (!REQUEST.equals(request.getAction())) return;
                String nonce = request.getStringExtra("nonce");
                if (nonce == null || !nonce.matches("[a-f0-9-]{36}")) return;
                Intent response = new Intent(RESPONSE).setPackage("com.hchen.appretention")
                    .putExtra("nonce", nonce).putExtra("version", BuildConfig.VERSION_CODE)
                    .putExtra("count", HookDiagnostics.total())
                    .putExtra("killshield", HookDiagnostics.count("KillShieldOpt"));
                context.sendBroadcast(response);
            }
        };
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, new IntentFilter(REQUEST),
            "android.permission.DUMP", null, Context.RECEIVER_EXPORTED);
        else context.registerReceiver(receiver, new IntentFilter(REQUEST), "android.permission.DUMP", null);
        registered = true;
        XposedLog.logINoSave("HookStatus", "Status receiver ready; registration count=" + HookDiagnostics.total());
    }
}
