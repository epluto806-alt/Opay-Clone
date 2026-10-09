package com.opay.clone

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class PersonalInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_info)

        val phone = intent.getStringExtra("phone") ?: AccountStore.getCurrentPhone(this)
        val etFirst = findViewById<EditText>(R.id.etFirstName)
        val etMiddle = findViewById<EditText>(R.id.etMiddleName)
        val etLast = findViewById<EditText>(R.id.etLastName)

        // Pre-fill from the resolved OPay name (passed by SignupActivity)
        intent.getStringExtra("resolvedName")?.trim()?.takeIf { it.isNotEmpty() }?.let { resolved ->
            val parts = Safe.splitName(resolved).toList().filter { it.isNotBlank() }
            when (parts.size) {
                0 -> {}
                1 -> {
                    etFirst.setText(parts[0])
                }
                2 -> {
                    etFirst.setText(parts[0])
                    etLast.setText(parts[1])
                }
                else -> {
                    etFirst.setText(parts.first())
                    etMiddle.setText(parts.subList(1, parts.size - 1).joinToString(" "))
                    etLast.setText(parts.last())
                }
            }
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnNext).setOnClickListener {
            val first = etFirst.text.toString().trim()
            val middle = etMiddle.text.toString().trim()
            val last = etLast.text.toString().trim()

            if (first.isEmpty()) {
                Toast.makeText(this, "Please enter your first name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (last.isEmpty()) {
                Toast.makeText(this, "Please enter your last name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fullName = listOf(first, middle, last).filter { it.isNotEmpty() }.joinToString(" ")
            AppPrefs.setName(this, fullName)

            if (!AccountStore.exists(this, phone)) {
                AccountStore.register(this, phone, fullName, "")
                AccountStore.bindDevice(this, phone, DeviceId.get(this))
            } else {
                AccountStore.updateName(this, phone, fullName)
            }
            AccountStore.setCurrentPhone(this, phone)

            val i = Intent(this, ResidentialAddressActivity::class.java)
            i.putExtra("phone", phone)
            startActivity(i)
        }
    }
}