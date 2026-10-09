package com.opay.clone

import android.content.Context
import java.util.Locale

data class Account(
    val phone: String,
    val name: String,
    val balance: String,
    val password: String,
    val deviceId: String?,
    val pendingDeposit: Boolean
)

object AccountStore {
    private const val FILE = "account_store"
    private const val CURRENT = "current_phone"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ---- keys ----
    private fun kName(p: String) = "acc_${p}_name"
    private fun kBal(p: String)  = "acc_${p}_balance"
    private fun kPw(p: String)   = "acc_${p}_password"
    private fun kDev(p: String)  = "acc_${p}_device_id"
    private fun kPen(p: String)  = "acc_${p}_pending"
    private fun kExists(p: String) = "acc_${p}_exists"

    // ---- who is logged in right now ----
    fun getCurrentPhone(ctx: Context): String =
        prefs(ctx).getString(CURRENT, "") ?: ""

    fun setCurrentPhone(ctx: Context, phone: String) {
        prefs(ctx).edit().putString(CURRENT, phone.trim()).apply()
    }

    fun clearCurrent(ctx: Context) {
        prefs(ctx).edit().remove(CURRENT).apply()
    }

    // ---- account existence ----
    fun exists(ctx: Context, phone: String): Boolean =
        prefs(ctx).getBoolean(kExists(normalize(phone)), false)

    // ---- register ----
    fun register(ctx: Context, phone: String, name: String, password: String) {
        val p = normalize(phone)
        prefs(ctx).edit()
            .putBoolean(kExists(p), true)
            .putString(kName(p), name)
            .putString(kPw(p), password)
            .putString(kBal(p), "0.00")
            .putString(kDev(p), "")
            .putBoolean(kPen(p), false)
            .apply()
    }

    // ---- read ----
    fun getAccount(ctx: Context, phone: String): Account? {
        val p = normalize(phone)
        val sp = prefs(ctx)
        if (!sp.getBoolean(kExists(p), false)) return null
        return Account(
            phone = p,
            name = sp.getString(kName(p), "") ?: "",
            balance = sp.getString(kBal(p), "0.00") ?: "0.00",
            password = sp.getString(kPw(p), "") ?: "",
            deviceId = sp.getString(kDev(p), "")?.takeIf { it.isNotEmpty() },
            pendingDeposit = sp.getBoolean(kPen(p), false)
        )
    }

    // ---- update ----
    fun updateName(ctx: Context, phone: String, name: String) {
        prefs(ctx).edit().putString(kName(normalize(phone)), name).apply()
    }

    fun updatePassword(ctx: Context, phone: String, password: String) {
        prefs(ctx).edit().putString(kPw(normalize(phone)), password).apply()
    }

    fun setBalance(ctx: Context, phone: String, amount: String) {
        prefs(ctx).edit().putString(kBal(normalize(phone)), amount).apply()
    }

    // add to existing balance
    fun creditByPhone(ctx: Context, phone: String, amount: Double) {
        val p = normalize(phone)
        val sp = prefs(ctx)
        if (!sp.getBoolean(kExists(p), false)) return
        val current = sp.getString(kBal(p), "0.00")?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        val newBal = current + amount
        sp.edit()
            .putString(kBal(p), String.format(Locale.US, "%.2f", newBal))
            .putBoolean(kPen(p), false)
            .apply()
    }

    // ---- device binding ----
    fun getBoundDevice(ctx: Context, phone: String): String? =
        prefs(ctx).getString(kDev(normalize(phone)), "")?.takeIf { it.isNotEmpty() }

    fun bindDevice(ctx: Context, phone: String, deviceId: String) {
        prefs(ctx).edit().putString(kDev(normalize(phone)), deviceId).apply()
    }

    /**
     * Returns true when the currently-running device is allowed to log in.
     * - Bound device is null  -> allowed (legacy / never bound).
     * - Bound device equals current device  -> allowed.
     * - Otherwise  -> blocked.
     */
    fun isDeviceAllowed(ctx: Context, phone: String): Boolean {
        val bound = getBoundDevice(ctx, phone) ?: return true
        val current = DeviceId.get(ctx)
        return bound == current
    }

    // ---- pending deposit flag ----
    fun setPending(ctx: Context, phone: String, value: Boolean) {
        prefs(ctx).edit().putBoolean(kPen(normalize(phone)), value).apply()
    }

    fun isPending(ctx: Context, phone: String): Boolean =
        prefs(ctx).getBoolean(kPen(normalize(phone)), false)

    // ---- helpers ----
    private fun normalize(phone: String): String =
        phone.replace("+234", "").replace(" ", "").replace("-", "").trim()

    fun hasAddress(ctx: android.content.Context, phone: String): Boolean {
        val prefs = ctx.getSharedPreferences("opay_accounts", android.content.Context.MODE_PRIVATE)
        return prefs.getString("addr_state_$phone", null) != null
    }

    fun saveAddress(
        ctx: android.content.Context,
        phone: String,
        state: String,
        lga: String,
        area: String,
        street: String
    ) {
        val prefs = ctx.getSharedPreferences("opay_accounts", android.content.Context.MODE_PRIVATE)
        prefs.edit()
            .putString("addr_state_$phone", state)
            .putString("addr_lga_$phone", lga)
            .putString("addr_area_$phone", area)
            .putString("addr_street_$phone", street)
            .apply()
    }

    // ---- payment pin ----
    private fun kPayPin(p: String) = "acc_${p}_paypin"

    fun setPaymentPin(ctx: Context, phone: String, pin: String) {
        prefs(ctx).edit().putString(kPayPin(normalize(phone)), pin).apply()
    }

    fun getPaymentPin(ctx: Context, phone: String): String =
        prefs(ctx).getString(kPayPin(normalize(phone)), "") ?: ""

    fun hasPaymentPin(ctx: Context, phone: String): Boolean =
        getPaymentPin(ctx, phone).length == 4
}

