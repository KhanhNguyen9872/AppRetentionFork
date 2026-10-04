package com.hchen.appretention.hook.system.opt;
/** Preserve TOP, visible, perceptible and foreground-service processes. */
public final class RestrictionPolicy {
    private RestrictionPolicy() {}
    public static boolean canTerminate(Integer processState, Integer adj) {
        return processState != null && adj != null && processState >= 7
            && processState <= 20 && adj >= 400 && adj <= 1000;
    }
}
