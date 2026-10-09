package com.opay.clone

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Global crash handler.
 *
 * - Catches every unhandled exception.
 * - Writes a full stack trace + device info to files/crash_log.txt in the
 *   app's private storage.
 * - Keeps only the last 5 crash entries so the file doesn't grow forever.
 * - Tries to relaunch the app on the login screen so the user isn't stuck
 *   staring at a black screen.
 * - Pull the log later with:
 *     adb shell run-as com.opay.clone cat files/crash_log.txt
 */
class CrashHandler private constructor(
    private val ctx: Context,
    private val previous: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            pw.flush()

            val header = buildString {
                append("========== CRASH ==========\n")
                append("Time   : ")
                append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                append("\n")
                append("Thread : ")
                append(t.name)
                append("\n")
                append("Device : ")
                append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
                append("\n")
                append("Android: ")
                append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")")
                append("\n")
                append("App    : ")
                append("com.opay.clone")
                append("\n")
                append("Message: ")
                append(e.message ?: "(no message)")
                append("\n")
                append("----------------------------------------\n")
                append(sw.toString())
                append("\n\n")
            }

            appendToLog(ctx, header)
        } catch (_: Throwable) {
            // Never crash inside the crash handler.
        }

        // Try to restart the app on the login screen so the user isn't stranded.
        try {
            val i = Intent(ctx, LoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            ctx.startActivity(i)
        } catch (_: Throwable) { /* ignore */ }

        // Let the platform do its default thing (close the process).
        if (previous != null) {
            previous.uncaughtException(t, e)
        } else {
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    companion object {
        private const val LOG_NAME = "crash_log.txt"
        private const val MAX_ENTRIES = 5
        private const val SEPARATOR = "\n<<<END-CRASH>>>\n"

        fun install(ctx: Context) {
            val prev = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(ctx.applicationContext, prev))
        }

        /** Full log file as a string, or empty if none. */
        fun readLog(ctx: Context): String {
            return try {
                val f = File(ctx.filesDir, LOG_NAME)
                if (f.exists()) f.readText() else ""
            } catch (_: Throwable) { "" }
        }

        /** Clear the crash log (e.g. from a hidden debug screen). */
        fun clearLog(ctx: Context) {
            try {
                File(ctx.filesDir, LOG_NAME).delete()
            } catch (_: Throwable) { }
        }

        private fun appendToLog(ctx: Context, text: String) {
            try {
                val f = File(ctx.filesDir, LOG_NAME)
                val existing = if (f.exists()) f.readText() else ""
                val combined = existing + text + SEPARATOR

                // Trim to last MAX_ENTRIES
                val parts = combined.split(SEPARATOR)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                val kept = if (parts.size > MAX_ENTRIES)
                    parts.subList(parts.size - MAX_ENTRIES, parts.size).joinToString(SEPARATOR + "\n")
                else
                    parts.joinToString(SEPARATOR + "\n")

                f.writeText(kept)
            } catch (_: Throwable) { }
        }
    }
}