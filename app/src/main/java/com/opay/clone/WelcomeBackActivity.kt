package com.opay.clone

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class WelcomeBackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome_back)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvPhone).text = AppPrefs.getPhone(this)
        findViewById<TextView>(R.id.tvForgot).setOnClickListener {
            val i = Intent(this, OtpActivity::class.java)
            i.putExtra("phone", AppPrefs.getPhone(this))
            startActivity(i)
        }

        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            val pwd = findViewById<EditText>(R.id.etPassword).text.toString().trim()
            val saved = AppPrefs.getPassword(this)
            if (pwd == saved) {
                val phone = AppPrefs.getPhone(this).trim()
                if (phone.isEmpty() || !AccountStore.isDeviceAllowed(this, phone)) {
                    android.app.AlertDialog.Builder(this)
                        .setTitle("Account Locked")
                        .setMessage("This account is bound to another device. Please sign in from the original device or create a new account.")
                        .setPositiveButton("OK") { _, _ ->
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    startActivity(Intent(this, HomeActivity::class.java))
                }
            } else {
                showPasswordIncorrectPopup()
            }
        }
    }

    private fun showPasswordIncorrectPopup() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_password_incorrect, null)
        val dialog = AlertDialog.Builder(this).setView(view).setCancelable(true).create()
        view.findViewById<Button>(R.id.btnRetry).setOnClickListener { dialog.dismiss() }
        view.findViewById<Button>(R.id.btnForgot).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }
}
