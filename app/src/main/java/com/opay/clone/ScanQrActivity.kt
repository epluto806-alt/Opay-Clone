package com.opay.clone

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class ScanQrActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr)

        // Only the back arrow is clickable  dialog and Album are inert
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }
}
