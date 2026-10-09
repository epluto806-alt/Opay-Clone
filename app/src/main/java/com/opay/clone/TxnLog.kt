package com.opay.clone

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TxnLog {
    private const val PREFS = "opay_txn_log"
    private const val KEY = "txn_entries"

    fun record(
        ctx: Context,
        type: String,
        title: String,
        amount: Double,
        recipient: String = "",
        senderName: String = "",
        account: String = "",
        status: String = "Successful"
    ): Int {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        val now = Date()
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(now)
        val display = SimpleDateFormat("MMM d, h:mm:ss a", Locale.US).format(now)

        val entry = JSONObject().apply {
            put("type", type)
            put("title", title)
            put("amount", amount)
            put("recipient", recipient)
            put("senderName", senderName)
            put("account", account)
            put("timestamp", iso)
            put("date", display)
            put("status", status)
        }
        arr.put(entry)
        prefs.edit().putString(KEY, arr.toString()).apply()
        return arr.length() - 1
    }

    fun all(ctx: Context): String {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY, "[]") ?: "[]"
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    // Raw JSON index (0 = oldest, length-1 = newest). What record() returns.
    fun getByIndex(ctx: Context, rawIndex: Int): org.json.JSONObject? {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        if (rawIndex < 0 || rawIndex >= arr.length()) return null
        return arr.optJSONObject(rawIndex)
    }

    // UI index (0 = newest). For history list taps.
    fun getByUiIndex(ctx: Context, uiIndex: Int): org.json.JSONObject? {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        val rawIndex = arr.length() - 1 - uiIndex
        if (rawIndex < 0 || rawIndex >= arr.length()) return null
        return arr.optJSONObject(rawIndex)
    }

    fun count(ctx: Context): Int {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return JSONArray(prefs.getString(KEY, "[]")).length()
    }
}