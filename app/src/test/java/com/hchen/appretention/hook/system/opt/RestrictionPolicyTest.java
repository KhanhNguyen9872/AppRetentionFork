package com.hchen.appretention.hook.system.opt;
import org.junit.Test;
import static org.junit.Assert.*;
public class RestrictionPolicyTest {
    @Test public void foregroundVisibleAndPerceptibleArePreserved() {
        assertFalse(RestrictionPolicy.canTerminate(2, 0));
        assertFalse(RestrictionPolicy.canTerminate(6, 100));
        assertFalse(RestrictionPolicy.canTerminate(7, 200));
    }
    @Test public void foregroundServiceAndTopSleepingArePreserved() {
        assertFalse(RestrictionPolicy.canTerminate(4, 200));
        assertFalse(RestrictionPolicy.canTerminate(6, 400));
    }
    @Test public void unknownImportanceIsPreserved() {
        assertFalse(RestrictionPolicy.canTerminate(null, 900));
        assertFalse(RestrictionPolicy.canTerminate(16, null));
        assertFalse(RestrictionPolicy.canTerminate(21, 900));
    }
    @Test public void genuinelyBackgroundCandidatesCanBeTerminated() {
        assertTrue(RestrictionPolicy.canTerminate(10, 500));
        assertTrue(RestrictionPolicy.canTerminate(16, 900));
    }
}
