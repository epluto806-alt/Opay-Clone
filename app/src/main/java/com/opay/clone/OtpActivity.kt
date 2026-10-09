package com.opay.clone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class OtpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // "Contact Support" clickable span  Telegram
        val supportTv = findViewById<TextView>(R.id.tvSupportRow)
        val full = "Did not receive the code? Contact Support or retry later."
        val span = SpannableString(full)
        val start = full.indexOf("Contact Support")
        val end = start + "Contact Support".length
        if (start >= 0) {
            span.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, R.color.opay_green)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            span.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/OnlyWebtools")))
                    } catch (_: Exception) {
                        Toast.makeText(this@OtpActivity, "Cannot open link", Toast.LENGTH_SHORT).show()
                    }
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        supportTv.text = span
        supportTv.movementMethod = LinkMovementMethod.getInstance()

        findViewById<MaterialButton>(R.id.btnVerify).setOnClickListener {
            val code = findViewById<EditText>(R.id.etOtp).text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(this, "Please enter the code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Activation gate removed  admin funds accounts manually.
            AppPrefs.markActivated(this)
            startActivity(Intent(this, ResidentialAddressActivity::class.java))
            finish()
        }
    }
}

