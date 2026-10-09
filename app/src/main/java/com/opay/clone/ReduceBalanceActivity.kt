package com.opay.clone

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ReduceBalanceActivity : AppCompatActivity() {

    private lateinit var etAmount: EditText
    private lateinit var tvBalance: TextView
    private lateinit var btnConfirm: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reduce_balance)

        findViewById<ImageView>(R.id.btnBackReduce).setOnClickListener { finish() }

        etAmount = findViewById(R.id.etReduceAmount)
        tvBalance = findViewById(R.id.tvReduceBalance)
        btnConfirm = findViewById(R.id.btnReduceConfirm)

        refreshBalance()

        etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { updateConfirmState() }
        })

        btnConfirm.setOnClickListener {
            val raw = etAmount.text.toString().trim()
            val amt = raw.toDoubleOrNull() ?: 0.0
            if (amt <= 0.0) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val phone = AccountStore.getCurrentPhone(this)
            if (phone.isEmpty()) {
                Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val acc = AccountStore.getAccount(this, phone)
            val current = acc?.balance?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            if (amt > current) {
                Toast.makeText(this, "Amount exceeds available balance", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newBal = (current - amt).coerceAtLeast(0.0)
            AccountStore.setBalance(this, phone, String.format(Locale.US, "%.2f", newBal))
            Toast.makeText(this, "Removed \u20A6" + String.format("%,.2f", amt), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun refreshBalance() {
        val phone = AccountStore.getCurrentPhone(this)
        val acc = if (phone.isNotEmpty()) AccountStore.getAccount(this, phone) else null
        val d = acc?.balance?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        tvBalance.text = "\u20A6" + String.format("%,.2f", d)
        updateConfirmState()
    }

    private fun updateConfirmState() {
        val raw = etAmount.text.toString().trim()
        val amt = raw.toDoubleOrNull() ?: 0.0
        val valid = amt > 0.0
        btnConfirm.isEnabled = valid
        btnConfirm.alpha = if (valid) 1f else 0.4f
    }
}