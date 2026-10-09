package com.opay.clone

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class LoginIssuesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_issues)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
    }
}
