package com.opay.clone

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

object PaymentPinDialog {

    fun show(ctx: Context, onSuccess: () -> Unit) {
        val dialog = Dialog(ctx)
        dialog.setContentView(R.layout.dialog_enter_payment_pin)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (ctx.resources.displayMetrics.widthPixels).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(android.view.Gravity.BOTTOM)
        dialog.setCancelable(false)

        val dots = listOf(
            dialog.findViewById<ImageView>(R.id.dot_1),
            dialog.findViewById<ImageView>(R.id.dot_2),
            dialog.findViewById<ImageView>(R.id.dot_3),
            dialog.findViewById<ImageView>(R.id.dot_4)
        )
        val boxes = listOf(
            dialog.findViewById<android.view.View>(R.id.pin_1),
            dialog.findViewById<android.view.View>(R.id.pin_2),
            dialog.findViewById<android.view.View>(R.id.pin_3),
            dialog.findViewById<android.view.View>(R.id.pin_4)
        )

        var pin = ""

        fun render() {
            for (i in 0..3) {
                dots[i].visibility = if (i < pin.length) View.VISIBLE else View.INVISIBLE
                boxes[i].setBackgroundResource(
                    if (i == pin.length) R.drawable.bg_pin_box_focus else R.drawable.bg_pin_box_idle
                )
            }
        }

        fun submitIfComplete() {
            if (pin.length == 4) {
                val phone = AccountStore.getCurrentPhone(ctx)
                val stored = if (phone.isNotEmpty()) AccountStore.getPaymentPin(ctx, phone) else ""
                if (stored.isEmpty()) {
                    Toast.makeText(ctx, "No Payment PIN set. Go to Security Center first.", Toast.LENGTH_LONG).show()
                    pin = ""
                    render()
                    return
                }
                if (pin == stored) {
                    dialog.dismiss()
                    onSuccess()
                } else {
                    Toast.makeText(ctx, "Incorrect PIN. Try again.", Toast.LENGTH_SHORT).show()
                    pin = ""
                    render()
                }
            }
        }

        val keys = mapOf(
            R.id.dkey1 to "1", R.id.dkey2 to "2", R.id.dkey3 to "3",
            R.id.dkey4 to "4", R.id.dkey5 to "5", R.id.dkey6 to "6",
            R.id.dkey7 to "7", R.id.dkey8 to "8", R.id.dkey9 to "9",
            R.id.dkey0 to "0"
        )
        for ((id, digit) in keys) {
            dialog.findViewById<TextView>(id).setOnClickListener {
                if (pin.length < 4) pin += digit
                render()
                submitIfComplete()
            }
        }
        dialog.findViewById<TextView>(R.id.dkeyDel).setOnClickListener {
            if (pin.isNotEmpty()) pin = pin.dropLast(1)
            render()
        }
        dialog.findViewById<ImageView>(R.id.btnClosePin).setOnClickListener { dialog.dismiss() }
        dialog.findViewById<TextView>(R.id.tvForgotPinDlg).setOnClickListener {
            Toast.makeText(ctx, "Reset flow not wired yet", Toast.LENGTH_SHORT).show()
        }

        render()
        dialog.show()
    }
}