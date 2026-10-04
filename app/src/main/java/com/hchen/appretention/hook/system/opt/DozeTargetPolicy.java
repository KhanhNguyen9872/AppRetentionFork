package com.hchen.appretention.hook.system.opt;
import java.util.function.Predicate;
/** Fail closed for unknown IDs and shared IDs containing an excluded package. */
public final class DozeTargetPolicy {
    private DozeTargetPolicy() {}
    public static boolean shouldExempt(String[] packages, Predicate<String> eligible) {
        if (packages == null || packages.length == 0) return false;
        for (String pkg : packages) {
            if (pkg == null || pkg.isEmpty() || !eligible.test(pkg)) return false;
        }
        return true;
    }
}
