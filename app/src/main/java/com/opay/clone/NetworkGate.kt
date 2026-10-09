package com.opay.clone

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView

object NetworkGate {

    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun showNoInternetDialog(context: Context) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_no_internet)
        dialog.setCancelable(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val confirm = dialog.findViewById<TextView>(R.id.btnNoNetConfirm)
        confirm.setOnClickListener { dialog.dismiss() }

        dialog.show()

        // Force width to ~85% of the screen so the card looks proper
        val dm = context.resources.displayMetrics
        val width = (dm.widthPixels * 0.85).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun requireOnline(context: Context): Boolean {
        return if (isOnline(context)) {
            true
        } else {
            showNoInternetDialog(context)
            false
        }
    }
}
