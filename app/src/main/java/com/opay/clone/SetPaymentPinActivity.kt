package com.opay.clone

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SetPaymentPinActivity : AppCompatActivity() {

    private var pin1 = ""
    private var pin2 = ""
    private var enteringFirst = true  // true = typing Set PIN; false = typing Confirm PIN

    private lateinit var boxes1: List<View>
    private lateinit var boxes2: List<View>
    private lateinit var dots1: List<View>
    private lateinit var dots2: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_payment_pin)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        boxes1 = listOf(
            findViewById(R.id.pin1_1), findViewById(R.id.pin1_2),
            findViewById(R.id.pin1_3), findViewById(R.id.pin1_4)
        )
        boxes2 = listOf(
            findViewById(R.id.pin2_1), findViewById(R.id.pin2_2),
            findViewById(R.id.pin2_3), findViewById(R.id.pin2_4)
        )
        dots1 = listOf(
            findViewById(R.id.dot1_1), findViewById(R.id.dot1_2),
            findViewById(R.id.dot1_3), findViewById(R.id.dot1_4)
        )
        dots2 = listOf(
            findViewById(R.id.dot2_1), findViewById(R.id.dot2_2),
            findViewById(R.id.dot2_3), findViewById(R.id.dot2_4)
        )

        val confirm = findViewById<TextView>(R.id.btnConfirmPin)
        confirm.setOnClickListener {
            if (pin1.length == 4 && pin2.length == 4 && pin1 == pin2) {
                val phone = AccountStore.getCurrentPhone(this)
                if (phone.isNotEmpty()) {
                    AccountStore.setPaymentPin(this, phone, pin1)
                    Toast.makeText(this, "Payment PIN set successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "No logged-in user", Toast.LENGTH_SHORT).show()
                }
            } else if (pin1 != pin2) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show()
                pin1 = ""
                pin2 = ""
                enteringFirst = true
                render()
            }
        }

        val keys = mapOf(
            R.id.key1 to "1", R.id.key2 to "2", R.id.key3 to "3",
            R.id.key4 to "4", R.id.key5 to "5", R.id.key6 to "6",
            R.id.key7 to "7", R.id.key8 to "8", R.id.key9 to "9",
            R.id.key0 to "0"
        )
        for ((id, digit) in keys) {
            findViewById<TextView>(id).setOnClickListener { onDigit(digit) }
        }
        findViewById<TextView>(R.id.keyDel).setOnClickListener { onDelete() }
    }

    private fun onDigit(d: String) {
        if (enteringFirst) {
            if (pin1.length < 4) pin1 += d
            if (pin1.length == 4) enteringFirst = false
        } else {
            if (pin2.length < 4) pin2 += d
        }
        render()
    }

    private fun onDelete() {
        if (enteringFirst) {
            if (pin1.isNotEmpty()) pin1 = pin1.dropLast(1)
        } else {
            if (pin2.isNotEmpty()) pin2 = pin2.dropLast(1)
            if (pin2.isEmpty()) enteringFirst = true
        }
        render()
    }

    private fun render() {
        for (i in 0..3) {
            dots1[i].visibility = if (i < pin1.length) View.VISIBLE else View.INVISIBLE
            dots2[i].visibility = if (i < pin2.length) View.VISIBLE else View.INVISIBLE
        }
        val confirm = findViewById<TextView>(R.id.btnConfirmPin)
        val ok = pin1.length == 4 && pin2.length == 4 && pin1 == pin2
        confirm.isEnabled = ok
        confirm.alpha = if (ok) 1f else 0.4f
    }
}