package com.hchen.appretention.ui;
/** Module-scoped view clearing without deleting another process's open file. */
public final class LogSnapshot {
    private LogSnapshot() {}
    public static String after(String snapshot, long cutoffMillis) {
        StringBuilder result = new StringBuilder();
        boolean include = false;
        for (String line : snapshot.split("\\R")) {
            if (line.startsWith("---------")) { include = false; continue; }
            int space = line.indexOf(' ');
            try {
                if (space <= 0) throw new NumberFormatException();
                double time = Double.parseDouble(line.substring(0, space));
                include = Double.isFinite(time) && time * 1000 > cutoffMillis;
            } catch (NumberFormatException ignored) {
                // Continuation lines (including stack traces) inherit the preceding record.
            }
            if (include) result.append(line).append('\n');
        }
        return result.toString();
    }
}
