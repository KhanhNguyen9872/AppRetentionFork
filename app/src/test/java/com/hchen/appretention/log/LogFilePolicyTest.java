package com.hchen.appretention.log;
import java.io.*;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class LogFilePolicyTest {
    @Test public void filenamesCannotEscapeLogDirectory() {
        for (String value : new String[]{"../escape", "a/b", ".", "..", "a\\b", "", "/absolute"}) assertFalse(LogFilePolicy.validName(value));
        assertFalse(LogFilePolicy.validName(null)); assertTrue(LogFilePolicy.validName("AndroidV.log"));
    }
    @Test public void resetsOnlySelectedFileAndPreservesAppendWriter() throws Exception {
        Path directory = Files.createTempDirectory("appretention-log");
        File first = directory.resolve("first.log").toFile();
        File second = directory.resolve("second.log").toFile();
        try {
            Files.write(first.toPath(), "old".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Files.write(second.toPath(), "other".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            try (FileWriter append = new FileWriter(first, true)) {
                LogFilePolicy.truncate(first); append.write("new"); append.flush();
            }
            assertEquals("new", new String(Files.readAllBytes(first.toPath()), java.nio.charset.StandardCharsets.UTF_8));
            assertEquals("other", new String(Files.readAllBytes(second.toPath()), java.nio.charset.StandardCharsets.UTF_8));
        } finally { Files.deleteIfExists(first.toPath()); Files.deleteIfExists(second.toPath()); Files.deleteIfExists(directory); }
    }
}
