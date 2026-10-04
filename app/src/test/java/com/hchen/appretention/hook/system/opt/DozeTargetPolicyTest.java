package com.hchen.appretention.hook.system.opt;
import org.junit.Test;
import static org.junit.Assert.*;
public class DozeTargetPolicyTest {
    @Test public void unknownIdsFailClosed() {
        assertFalse(DozeTargetPolicy.shouldExempt(null, p -> true));
        assertFalse(DozeTargetPolicy.shouldExempt(new String[0], p -> true));
    }
    @Test public void eligibleKnownPackageIsExempt() { assertTrue(DozeTargetPolicy.shouldExempt(new String[]{"normal"}, p -> true)); }
    @Test public void restrictedAndAppRetentionAreExcluded() {
        assertFalse(DozeTargetPolicy.shouldExempt(new String[]{"restricted"}, p -> !"restricted".equals(p)));
        assertFalse(DozeTargetPolicy.shouldExempt(new String[]{"com.hchen.appretention"}, p -> !"com.hchen.appretention".equals(p)));
    }
    @Test public void sharedIdsFailClosedIfAnyPackageIsExcluded() {
        assertFalse(DozeTargetPolicy.shouldExempt(new String[]{"normal", "restricted"}, p -> !"restricted".equals(p)));
        assertFalse(DozeTargetPolicy.shouldExempt(new String[]{null}, p -> true));
    }
}
