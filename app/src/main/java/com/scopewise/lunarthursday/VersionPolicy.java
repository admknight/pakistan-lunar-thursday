package com.scopewise.lunarthursday;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Restrictive semantic-version parser for releases from our own GitHub repository. */
final class VersionPolicy {
    private static final Pattern VERSION = Pattern.compile(
            "^v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z.-]+))?$", Pattern.CASE_INSENSITIVE);

    private VersionPolicy() { }

    /** Returns positive if candidate is newer than installed. Invalid tags are rejected. */
    static int compare(String candidate, String installed) {
        long[] a = parse(candidate);
        long[] b = parse(installed);
        if (a == null || b == null) return Integer.MIN_VALUE;
        for (int i = 0; i < 3; i++) {
            if (a[i] != b[i]) return a[i] > b[i] ? 1 : -1;
        }
        // Stable of the same version is preferred to pre-release (if used later).
        if (a[3] != b[3]) return a[3] > b[3] ? 1 : -1;
        return 0;
    }

    static boolean validTag(String tag) {
        return parse(tag) != null;
    }

    /** Strip leading 'v' and pre-release suffix for user-facing version labels. */
    static String display(String tag) {
        if (!validTag(tag)) return "";
        String t = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
        return t;
    }

    private static long[] parse(String text) {
        if (text == null) return null;
        Matcher m = VERSION.matcher(text.trim());
        if (!m.matches()) return null;
        try {
            long[] result = new long[4];
            for (int i = 0; i < 3; i++) result[i] = Long.parseLong(m.group(i + 1));
            // Allow numbers only up to 32-bit values to reject nonsensical version tags.
            for (int i = 0; i < 3; i++) if (result[i] > Integer.MAX_VALUE) return null;
            result[3] = m.group(4) == null ? 1L : 0L;
            return result;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
