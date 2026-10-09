package com.opay.clone

import android.content.Context
import android.content.SharedPreferences
import java.io.File

object AppPrefs {
    private const val NAME = "opay_clone_prefs"
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getPhone(ctx: Context): String = prefs(ctx).getString("phone", "") ?: ""
    fun getPassword(ctx: Context): String = prefs(ctx).getString("password", "") ?: ""
    fun getName(ctx: Context): String = prefs(ctx).getString("display_name", "") ?: ""
    fun getBalance(ctx: Context): String = prefs(ctx).getString("balance", "0.00") ?: "0.00"

    fun setAccount(ctx: Context, phone: String, password: String) {
        prefs(ctx).edit()
            .putString("phone", phone)
            .putString("password", password)
            .putBoolean("activated", false)   // not activated until code verified
            .apply()
    }
    fun setName(ctx: Context, name: String) { prefs(ctx).edit().putString("display_name", name).apply() }
    fun setBalance(ctx: Context, amount: String) { prefs(ctx).edit().putString("balance", amount).apply() }
    fun clear(ctx: Context) { prefs(ctx).edit().clear().apply() }

    fun isBalanceHidden(ctx: Context): Boolean = prefs(ctx).getBoolean("balance_hidden", false)
    fun setBalanceHidden(ctx: Context, hidden: Boolean) { prefs(ctx).edit().putBoolean("balance_hidden", hidden).apply() }

    fun isBackgrounded(ctx: Context): Boolean = prefs(ctx).getBoolean("app_backgrounded", false)
    fun markBackgrounded(ctx: Context) { prefs(ctx).edit().putBoolean("app_backgrounded", true).apply() }
    fun clearBackgrounded(ctx: Context) { prefs(ctx).edit().putBoolean("app_backgrounded", false).apply() }

    // ---- Activation gate ----
    fun isActivated(ctx: Context): Boolean =
        prefs(ctx).getBoolean("activated", false)

    fun markActivated(ctx: Context) {
        prefs(ctx).edit().putBoolean("activated", true).apply()
    }

    // ---- Code pool (file-based, single-use) ----
    private fun codesFile(ctx: Context): File = File(ctx.filesDir, "activation_codes.txt")

    fun isCodeValid(ctx: Context, code: String): Boolean {
        val f = codesFile(ctx); if (!f.exists()) return false
        return try { f.useLines { lines -> lines.any { it.trim() == code } } }
        catch (_: Exception) { false }
    }
    fun consumeCode(ctx: Context, code: String) {
        val f = codesFile(ctx); if (!f.exists()) return
        try {
            val remaining = f.readLines().filterNot { it.trim() == code }
            f.writeText(remaining.joinToString("\n"))
            File(ctx.filesDir, "activation_codes_used.txt").appendText(code + "\n")
        } catch (_: Exception) {}
    }
    fun countCodes(ctx: Context): Int {
        val f = codesFile(ctx)
        return if (!f.exists()) 0 else try { f.readLines().size } catch (_: Exception) { 0 }
    }
}
