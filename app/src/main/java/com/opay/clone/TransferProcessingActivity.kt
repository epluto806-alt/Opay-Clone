package com.opay.clone

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class TransferProcessingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_processing)

        val account     = intent.getStringExtra("extra_account")       ?: ""
        val bankCode    = intent.getStringExtra("extra_bank_code")     ?: ""
        val bankName    = intent.getStringExtra("extra_bank_name")     ?: ""
        val resolvedName= intent.getStringExtra("extra_resolved_name") ?: ""
        val amount      = intent.getDoubleExtra("extra_amount", 0.0)

        findViewById<ImageView>(R.id.btnBackProc).setOnClickListener { }

        val ring = findViewById<ImageView>(R.id.ivProcessingRing)
        ring?.let {
            val spin = android.view.animation.RotateAnimation(
                0f, 360f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 1200
                repeatCount = android.view.animation.Animation.INFINITE
                interpolator = android.view.animation.LinearInterpolator()
            }
            it.startAnimation(spin)
        }



        Handler(Looper.getMainLooper()).postDelayed({
            val i = Intent(this, TransferSuccessActivity::class.java)
            i.putExtra("extra_account", account)
            i.putExtra("extra_bank_code", bankCode)
            i.putExtra("extra_bank_name", bankName)
            i.putExtra("extra_resolved_name", resolvedName)
            i.putExtra("extra_amount", amount)
            startActivity(i)
            finish()
        }, 3500)
    }
}