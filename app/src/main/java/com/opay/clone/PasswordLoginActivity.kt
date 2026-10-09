package com.opay.clone

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class PasswordLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_login)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Show saved phone under the avatar
        val phone = AppPrefs.getPhone(this).trim().replace("+234", "").trim()
        findViewById<TextView>(R.id.tvPhone).text = phone

        findViewById<MaterialButton>(R.id.btnLogIn).setOnClickListener {
            val entered = findViewById<EditText>(R.id.etPassword).text.toString().trim()
            val saved = AppPrefs.getPassword(this)

            if (entered.isEmpty()) {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (entered != saved) {
                Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val lockMode = intent.getBooleanExtra("lock_mode", false)
            if (lockMode) {
                finish()
            } else {
                val _phone = AppPrefs.getPhone(this).trim()
                if (_phone.isNotEmpty() && !AccountStore.isDeviceAllowed(this, _phone)) {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Account Locked")
                        .setMessage("This account is bound to another device. Please sign in from the original device or create a new account.")
                        .setPositiveButton("OK") { _, _ ->
                            startActivity(Intent(this, LoginActivity::class.java))
                            finishAffinity()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    startActivity(Intent(this, HomeActivity::class.java))
                }
                finishAffinity()
            }
        }

        // Forgot Password  dead for now
        findViewById<TextView>(R.id.tvForgot).setOnClickListener { /* no-op */ }
    }
}
