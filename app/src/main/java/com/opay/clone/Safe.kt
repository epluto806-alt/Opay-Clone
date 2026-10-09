package com.opay.clone

import android.content.Intent

/**
 * Safe.kt — tiny utilities so no screen ever crashes from a null extra,
 * an empty string, a bad index, or a bad number.
 *
 * Every function here is total: it always returns a value, never throws.
 */
object Safe {

    /** Intent.getStringExtra that never returns null. Empty string if missing. */
    fun str(intent: Intent?, key: String, default: String = ""): String {
        return try {
            intent?.getStringExtra(key)?.trim() ?: default
        } catch (_: Throwable) { default }
    }

    /** Intent.getIntExtra that never throws. */
    fun int(intent: Intent?, key: String, default: Int = 0): Int {
        return try {
            intent?.getIntExtra(key, default) ?: default
        } catch (_: Throwable) { default }
    }

    /** String or fallback if null/blank. */
    fun nz(s: String?, fallback: String = ""): String =
        if (s.isNullOrBlank()) fallback else s

    /** Split a name on whitespace and return [first, middle, last] with safe bounds. */
    fun splitName(full: String?): Triple<String, String, String> {
        val parts = (full ?: "").trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when (parts.size) {
            0 -> Triple("", "", "")
            1 -> Triple(parts[0], "", "")
            2 -> Triple(parts[0], "", parts[1])
            else -> Triple(parts[0], parts[1], parts.drop(2).joinToString(" "))
        }
    }

    /** Parse a decimal amount from user input. Never throws. */
    fun amount(s: String?): Double {
        return try {
            val cleaned = (s ?: "").replace(",", "").replace("\u20A6", "").trim()
            if (cleaned.isEmpty()) 0.0 else cleaned.toDouble()
        } catch (_: Throwable) { 0.0 }
    }

    /** Parse an Int from a string. Never throws. */
    fun intOf(s: String?, default: Int = 0): Int {
        return try {
            s?.trim()?.toInt() ?: default
        } catch (_: Throwable) { default }
    }

    /** Run a block of code, swallow every exception. For "fire and forget" paths. */
    inline fun <T> tryOr(default: T, block: () -> T): T {
        return try { block() } catch (_: Throwable) { default }
    }

    /** Run a block, do nothing if it throws. For UI side-effects. */
    inline fun silent(block: () -> Unit) {
        try { block() } catch (_: Throwable) { }
    }
}