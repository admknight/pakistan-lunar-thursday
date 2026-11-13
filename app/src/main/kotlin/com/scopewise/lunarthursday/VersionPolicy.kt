package com.scopewise.lunarthursday

import java.util.regex.Pattern

/** Restrictive semantic-version parser for releases from our own GitHub repository. */
object VersionPolicy {
    private val VERSION: Pattern = Pattern.compile(
        "^v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z.-]+))?$", Pattern.CASE_INSENSITIVE)

    /** Returns positive if candidate is newer than installed. Invalid tags are rejected. */
    @JvmStatic
    fun compare(candidate: String?, installed: String?): Int {
        val a = parse(candidate)
        val b = parse(installed)
        if (a == null || b == null) return Int.MIN_VALUE
        for (i in 0 until 3) {
            if (a[i] != b[i]) return if (a[i] > b[i]) 1 else -1
        }
        // Stable of the same version is preferred to pre-release (if used later).
        if (a[3] != b[3]) return if (a[3] > b[3]) 1 else -1
        return 0
    }

    @JvmStatic
    fun validTag(tag: String?): Boolean = parse(tag) != null

    /** Strip leading 'v' and pre-release suffix for user-facing version labels. */
    @JvmStatic
    fun display(tag: String?): String {
        if (tag == null || !validTag(tag)) return ""
        return if (tag.startsWith("v") || tag.startsWith("V")) tag.substring(1) else tag
    }

    private fun parse(text: String?): LongArray? {
        if (text == null) return null
        // Same trimming as Java's String.trim(): code points up to U+0020.
        val m = VERSION.matcher(text.trim { it <= ' ' })
        if (!m.matches()) return null
        return try {
            val result = LongArray(4)
            for (i in 0 until 3) result[i] = m.group(i + 1).toLong()
            // Allow numbers only up to 32-bit values to reject nonsensical version tags.
            for (i in 0 until 3) if (result[i] > Int.MAX_VALUE) return null
            result[3] = if (m.group(4) == null) 1L else 0L
            result
        } catch (ex: NumberFormatException) {
            null
        }
    }
}
