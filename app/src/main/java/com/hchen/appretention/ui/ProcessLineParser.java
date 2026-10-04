package com.hchen.appretention.ui;
import java.util.Locale;
/** Parse from the edges: Android process names may contain multiple colons. */
public final class ProcessLineParser {
    private ProcessLineParser() {}
    public static RootTool.ProcessInfo parse(String line) {
        if (line == null) return null;
        int first = line.indexOf(':');
        int last = line.lastIndexOf(':');
        int adjSeparator = last < 0 ? -1 : line.lastIndexOf(':', last - 1);
        if (first <= 0 || adjSeparator <= first || last <= adjSeparator) return null;
        try {
            int pid = Integer.parseInt(line.substring(0, first).trim());
            String name = line.substring(first + 1, adjSeparator).trim();
            int end = name.indexOf(' ');
            if (end >= 0) name = name.substring(0, end);
            end = name.indexOf('\0');
            if (end >= 0) name = name.substring(0, end);
            int adj = Integer.parseInt(line.substring(adjSeparator + 1, last).trim());
            if (pid <= 0 || adj < -1000 || adj > 1000 || name.isEmpty()
                || name.startsWith("/") || name.startsWith("[")) return null;
            String rss = line.substring(last + 1).trim().toLowerCase(Locale.ROOT);
            double scale = 1024;
            if (rss.endsWith("g")) scale = 1024d * 1024 * 1024;
            else if (rss.endsWith("m")) scale = 1024d * 1024;
            if (rss.endsWith("g") || rss.endsWith("m") || rss.endsWith("k")) rss = rss.substring(0, rss.length() - 1);
            double bytes = Double.parseDouble(rss) * scale;
            if (!Double.isFinite(bytes) || bytes < 0 || bytes > Long.MAX_VALUE) return null;
            return new RootTool.ProcessInfo(pid, name, adj, (long) bytes);
        } catch (NumberFormatException error) { return null; }
    }
}
