package com.opay.clone

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvLoginLink).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnNext).setOnClickListener {
            if (!NetworkGate.requireOnline(this)) return@setOnClickListener

            val cbTerms = findViewById<CheckBox>(R.id.cbTerms)
            if (!cbTerms.isChecked) {
                Toast.makeText(this, "Please accept the Terms & Conditions to continue", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val phone = findViewById<EditText>(R.id.etPhone).text.toString().trim()
            if (phone.length < 10) {
                Toast.makeText(this, "Please enter a valid mobile number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save phone as current; name + password come later
            AppPrefs.setAccount(this, phone, "")
            AccountStore.setCurrentPhone(this, phone)
            AppPrefs.markActivated(this)

            // Fire name resolution for the phone (OPay bank code 999992).
            // Show loading state, then navigate. On failure still navigate with empty name.
            val btn = findViewById<MaterialButton>(R.id.btnNext)
            btn.isEnabled = false
            btn.text = "Resolving name..."

            BankResolver.resolve(phone, "999992") { resolvedName ->
                runOnUiThread {
                    btn.isEnabled = true
                    btn.text = "Next"
                    val i = Intent(this, PersonalInfoActivity::class.java)
                    i.putExtra("phone", phone)
                    if (!resolvedName.isNullOrBlank()) {
                        i.putExtra("resolvedName", resolvedName)
                    }
                    startActivity(i)
                }
            }
        }
    }
}