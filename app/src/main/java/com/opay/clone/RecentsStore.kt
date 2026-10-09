package com.opay.clone

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecentRecipient(
    val name: String,
    val account: String,
    val bankName: String,
    val bankCode: String,
    val isOpay: Boolean,
    val timestamp: Long
)

object RecentsStore {

    private const val PREFS = "opay_recents"
    private const val MAX_ITEMS = 20

    // Per-user key: recents are scoped to the currently logged-in phone.
    private fun keyFor(ctx: Context): String {
        val phone = AccountStore.getCurrentPhone(ctx)
        return if (phone.isEmpty()) "recent_list" else "recent_list_$phone"
    }

    fun all(ctx: Context): List<RecentRecipient> {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(keyFor(ctx), "[]") ?: "[]"
        val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray() }
        val out = mutableListOf<RecentRecipient>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out += RecentRecipient(
                name = o.optString("name"),
                account = o.optString("account"),
                bankName = o.optString("bankName"),
                bankCode = o.optString("bankCode"),
                isOpay = o.optBoolean("isOpay", false),
                timestamp = o.optLong("timestamp", 0L)
            )
        }
        return out.sortedByDescending { it.timestamp }
    }

    fun add(ctx: Context, r: RecentRecipient) {
        val existing = all(ctx).toMutableList()
        // Remove duplicate: same account + same bank
        existing.removeAll { it.account == r.account && it.bankCode == r.bankCode }
        // Add newest at front
        existing.add(0, r)
        // Trim
        val trimmed = existing.take(MAX_ITEMS)

        val arr = JSONArray()
        for (item in trimmed) {
            val o = JSONObject().apply {
                put("name", item.name)
                put("account", item.account)
                put("bankName", item.bankName)
                put("bankCode", item.bankCode)
                put("isOpay", item.isOpay)
                put("timestamp", item.timestamp)
            }
            arr.put(o)
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(keyFor(ctx), arr.toString()).apply()
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(keyFor(ctx)).apply()
    }
}