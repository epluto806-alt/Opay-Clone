package com.opay.clone

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

object DeviceId {
    @SuppressLint("HardwareIds")
    fun get(ctx: Context): String {
        return try {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
    }
}
