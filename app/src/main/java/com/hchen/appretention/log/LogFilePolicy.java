package com.hchen.appretention.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** File operations shared by logger reset and module-scoped UI clear. */
public final class LogFilePolicy {
    private LogFilePolicy() {}
    public static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9_-][A-Za-z0-9_.-]{0,95}");
    }
    public static void truncate(File file) throws IOException {
        // Preserve the inode: append writers held by other processes remain usable.
        try (FileOutputStream output = new FileOutputStream(file, false)) { output.flush(); }
    }
}
