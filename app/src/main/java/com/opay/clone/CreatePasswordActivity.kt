package com.opay.clone

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class CreatePasswordActivity : AppCompatActivity() {

    private lateinit var p: Array<EditText>
    private lateinit var q: Array<EditText>
    private lateinit var tvMismatch: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_password)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        tvMismatch = findViewById(R.id.tvMismatch)

        p = arrayOf(
            findViewById(R.id.p1), findViewById(R.id.p2), findViewById(R.id.p3),
            findViewById(R.id.p4), findViewById(R.id.p5), findViewById(R.id.p6)
        )
        q = arrayOf(
            findViewById(R.id.q1), findViewById(R.id.q2), findViewById(R.id.q3),
            findViewById(R.id.q4), findViewById(R.id.q5), findViewById(R.id.q6)
        )

        wireAutoAdvance(p, q)
        wireAutoAdvance(q, null)

        findViewById<MaterialButton>(R.id.btnConfirm).setOnClickListener {
            val pass1 = p.joinToString("") { it.text.toString() }
            val pass2 = q.joinToString("") { it.text.toString() }

            if (pass1.length != 6 || pass2.length != 6) {
                Toast.makeText(this, "Please enter all 6 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass1 != pass2) {
                tvMismatch.visibility = View.VISIBLE
                return@setOnClickListener
            }
            tvMismatch.visibility = View.GONE

            val phone = AccountStore.getCurrentPhone(this)
            if (phone.isEmpty()) {
                Toast.makeText(this, "Session expired. Please sign up again.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SignupActivity::class.java))
                finishAffinity()
                return@setOnClickListener
            }

            // Save password
            AppPrefs.setAccount(this, phone, pass1)
            if (AccountStore.exists(this, phone)) {
                AccountStore.updatePassword(this, phone, pass1)
            } else {
                AccountStore.register(this, phone, AppPrefs.getName(this), pass1)
                AccountStore.bindDevice(this, phone, DeviceId.get(this))
                // One-time welcome balance for newly registered users.
                // Fires only in the "new account" branch — no re-credit on login.
                AccountStore.setBalance(this, phone, "1000")
            }

            // Navigate to Home
            val i = Intent(this, HomeActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
            finishAffinity()
        }
    }

    private fun wireAutoAdvance(row: Array<EditText>, nextRow: Array<EditText>?) {
        for (i in row.indices) {
            row[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1) {
                        if (i < row.size - 1) row[i + 1].requestFocus()
                        else nextRow?.get(0)?.requestFocus()
                    } else if (s?.length == 0) {
                        if (i > 0) row[i - 1].requestFocus()
                    }
                }
            })
            row[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && event.action == android.view.KeyEvent.ACTION_DOWN && row[i].text.isEmpty() && i > 0) {
                    row[i - 1].requestFocus()
                    row[i - 1].text.clear()
                    true
                } else false
            }
        }
    }
}