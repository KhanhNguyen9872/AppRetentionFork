package com.hchen.appretention.ui;
import org.junit.Test;
import static org.junit.Assert.*;
public class ProcessLineParserTest {
    @Test public void mainProcess() {
        RootTool.ProcessInfo p = ProcessLineParser.parse("101:com.example.app:200:1024");
        assertNotNull(p); assertEquals(101, p.pid); assertEquals(200, p.adj);
        assertEquals(1048576L, p.memoryBytes);
    }
    @Test public void secondaryAndNestedProcessNames() {
        assertEquals("com.example.app:push", ProcessLineParser.parse("102:com.example.app:push:250:512").processName);
        assertEquals("com.example.app:worker:remote", ProcessLineParser.parse("103:com.example.app:worker:remote:900:512").processName);
    }
    @Test public void negativeSystemAdj() {
        assertEquals(-1000, ProcessLineParser.parse("1:init:-1000:64").adj);
    }
    @Test public void rssUnits() {
        assertEquals(1073741824L, ProcessLineParser.parse("1:pkg:0:1g").memoryBytes);
        assertEquals(1572864L, ProcessLineParser.parse("1:pkg:0:1.5m").memoryBytes);
        assertEquals(2048L, ProcessLineParser.parse("1:pkg:0:2k").memoryBytes);
    }
    @Test public void invalidRowsAreRejected() {
        for (String value : new String[]{"", "header", "0:pkg:200:1", "1:pkg:200:NaN",
            "1:pkg:200:Infinity", "1:pkg:200:-1", "1:pkg:200:1e40", "1:/bin/sh:0:1", "1:pkg:9999:1"}) {
            assertNull(value, ProcessLineParser.parse(value));
        }
        assertNull(ProcessLineParser.parse(null));
    }
}
