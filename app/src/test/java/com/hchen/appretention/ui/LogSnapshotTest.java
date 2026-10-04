package com.hchen.appretention.ui;
import org.junit.Test;
import static org.junit.Assert.*;
public class LogSnapshotTest {
    @Test public void markersAreNotLogs() { assertEquals("", LogSnapshot.after("--------- beginning of main\n", 0)); }
    @Test public void clearsOnlyOlderRecords() {
        String value = "1000.001 12 13 I AppRetention: old\n1000.003 12 13 I AppRetention: new\n";
        assertEquals("1000.003 12 13 I AppRetention: new\n", LogSnapshot.after(value, 1000002));
    }
    @Test public void keepsNewStackTraceAndDropsOldStackTrace() {
        String value = "1000.001 12 13 E AppRetention: old\nold stack\n1000.003 12 13 E AppRetention: new\nnew stack\n";
        assertEquals("1000.003 12 13 E AppRetention: new\nnew stack\n", LogSnapshot.after(value, 1000002));
    }
    @Test public void collectionErrorsDoNotPretendToBeRecords() { assertEquals("", LogSnapshot.after("permission denied\n", 0)); }
    @Test public void initialSnapshotIncludesRecords() { assertTrue(LogSnapshot.after("1000.001 12 13 I AppRetention: boot\n", 0).contains("boot")); }
}
