package com.opay.clone

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class CustomerServiceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_service)

        // Only the back arrow is functional
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }
}
