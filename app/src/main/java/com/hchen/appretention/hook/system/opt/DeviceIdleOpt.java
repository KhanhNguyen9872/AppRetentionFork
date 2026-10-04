package com.hchen.appretention.hook.system.opt;

import static com.hchen.hooktool.core.CoreTool.findClassIfExists;
import static com.hchen.hooktool.core.CoreTool.hook;

import com.hchen.appretention.log.XposedLog;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.utils.SystemPropTool;

import java.lang.reflect.Method;

/**
 * Answers compatible Doze whitelist queries for eligible packages.
 * This does not guarantee exemption from every alarm, job, network or standby policy.
 *
 * @author Antigravity
 */
public final class DeviceIdleOpt {
    private static final String TAG = "DeviceIdleOpt";

    public static void init() {
        // Install while OFF; callback-time gates allow safe live toggles.

        Class<?> dicClass = findClassIfExists("com.android.server.DeviceIdleController");
        if (dicClass == null) {
            XposedLog.logW(TAG, "DeviceIdleController class not found!");
            return;
        }

        // Hook all public and internal whitelist check methods taking package name
        for (Method m : dicClass.getDeclaredMethods()) {
            String name = m.getName();
            Class<?>[] params = m.getParameterTypes();
            if ((name.startsWith("isPowerSaveWhitelist") || name.startsWith("isExceptIdlePowerSaveWhitelist"))
                    && params.length == 1 && params[0] == String.class
                    && (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)) {
                HookDiagnostics.install(TAG, m, new IHook() {
                    @Override
                    public void before() {
                        if (!isEnabled()) return;
                        String pkg = (String) getArg(0);
                        if (isTargetUserApp(pkg)) {
                            setResult(true);
                        }
                    }
                });
            }
        }

        // Hook inner LocalService (DeviceIdleInternal) if present
        for (Class<?> inner : dicClass.getDeclaredClasses()) {
            for (Method m : inner.getDeclaredMethods()) {
                String name = m.getName();
                Class<?>[] params = m.getParameterTypes();
                if ((name.startsWith("isAppOnWhitelist") || name.startsWith("isExceptIdlePowerSaveWhitelist"))
                        && params.length == 1 && (params[0] == int.class || params[0] == Integer.class)
                        && (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)) {
                    HookDiagnostics.install(TAG, m, new IHook() {
                        @Override
                        public void before() {
                            if (!isEnabled()) return;
                            int uid = (Integer) getArg(0);
                            if (isEligibleAppId(uid)) {
                                setResult(true);
                            }
                        }
                    });
                }
            }
        }

        HookDiagnostics.report(TAG);
    }

    private static boolean isEnabled() {
        return ForkFeatureGate.isEnabled()
            && SystemPropTool.getProp("persist.hchen.doze.opt.enable", true);
    }

    private static boolean isEligibleAppId(int appId) {
        // AppId overloads describe owner-user IDs, not arbitrary full multi-user UIDs.
        if (appId < 10000 || appId >= 100000) return false;
        try {
            Object pm = com.hchen.hooktool.core.CoreTool.callStaticMethod("android.app.ActivityThread", "getPackageManager");
            if (pm == null) return false;
            String[] packages = (String[]) com.hchen.hooktool.core.CoreTool.callMethod(pm, "getPackagesForUid", appId);
            return DozeTargetPolicy.shouldExempt(packages, DeviceIdleOpt::isTargetUserApp);
        } catch (Throwable ignored) { return false; }
    }

    private static boolean isTargetUserApp(String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        if (BackgroundRestrictOpt.isRestricted(packageName)) return false;
        if (BackgroundRestrictOpt.PACKAGE_APPRETENTION.equals(packageName)) return false;
        if ("android".equals(packageName)) return false;
        if (packageName.startsWith("com.android.providers.")) return false;
        if (packageName.startsWith("com.android.server.")) return false;
        return true;
    }
}
