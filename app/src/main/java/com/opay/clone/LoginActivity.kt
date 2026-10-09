package com.opay.clone

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvHelp).setOnClickListener {
            startActivity(Intent(this, LoginIssuesActivity::class.java))
        }

        val lostTv = findViewById<TextView>(R.id.tvLostLink)
        val lostText = lostTv.text.toString()
        val lostSpan = SpannableString(lostText)
        val idxLost = lostText.indexOf("Change Now")
        if (idxLost >= 0) {
            lostSpan.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.opay_green)),
                idxLost, idxLost + "Change Now".length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            lostSpan.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(this@LoginActivity, ChangePhoneActivity::class.java))
                }
            }, idxLost, idxLost + "Change Now".length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            lostTv.movementMethod = LinkMovementMethod.getInstance()
        }
        lostTv.text = lostSpan

        val signupTv = findViewById<TextView>(R.id.tvSignupRow)
        val signupText = signupTv.text.toString()
        val signupSpan = SpannableString(signupText)
        val idxLink = signupText.indexOf("Click here to get one")
        if (idxLink >= 0) {
            signupSpan.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.opay_green)),
                idxLink, idxLink + "Click here to get one".length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            signupSpan.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(this@LoginActivity, SignupActivity::class.java))
                }
            }, idxLink, idxLink + "Click here to get one".length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            signupTv.movementMethod = LinkMovementMethod.getInstance()
        }
        signupTv.text = signupSpan

        findViewById<MaterialButton>(R.id.btnNext).setOnClickListener {
            if (!NetworkGate.requireOnline(this)) return@setOnClickListener
            val phone = findViewById<EditText>(R.id.etLoginField).text.toString().trim()

            // Account must be fully registered: exists + address saved + password set.
            val exists = AccountStore.exists(this, phone)
            val hasAddr = exists && AccountStore.hasAddress(this, phone)
            val hasPw   = exists && !(AccountStore.getAccount(this, phone)?.password ?: "").isEmpty()

            if (phone.isEmpty() || !exists || !hasAddr || !hasPw) {
                showNotRegisteredPopup(phone)
            } else {
                // Device binding check (Stage 2)
                val boundDevice = AccountStore.getBoundDevice(this, phone)
                val thisDevice  = DeviceId.get(this)

                if (boundDevice == null) {
                    // Legacy account without a bound device  bind it now
                    AccountStore.bindDevice(this, phone, thisDevice)
                    AccountStore.setCurrentPhone(this, phone)
                    startActivity(Intent(this, WelcomeBackActivity::class.java))
                } else if (boundDevice == thisDevice) {
                    // Same device  allowed
                    AccountStore.setCurrentPhone(this, phone)
                    startActivity(Intent(this, WelcomeBackActivity::class.java))
                } else {
                    // Different device  block
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Account Locked")
                        .setMessage("These credentials are already registered on another device. Please create a new account.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun showNotRegisteredPopup(entered: String) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_not_registered, null)
        view.findViewById<TextView>(R.id.tvPopupBody).text =
            "This phone number ($entered) is not registered with OPay. Create a new OPay account now!"

        val dialog = AlertDialog.Builder(this).setView(view).setCancelable(true).create()

        view.findViewById<Button>(R.id.btnGetOtp).setOnClickListener {
            dialog.dismiss()
            val i = Intent(this, OtpActivity::class.java)
            i.putExtra("phone", entered)
            startActivity(i)
        }
        view.findViewById<Button>(R.id.btnReenter).setOnClickListener { dialog.dismiss() }
        view.findViewById<ImageView>(R.id.btnClose).setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}


