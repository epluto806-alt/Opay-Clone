package com.opay.clone

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class FundingAdminActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_funding_admin)

        findViewById<ImageView>(R.id.btnFundBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnFundAdd).setOnClickListener {
            val phone = findViewById<EditText>(R.id.etFundPhone).text.toString().trim()
            val amountStr = findViewById<EditText>(R.id.etFundAmount).text.toString().trim()
            val amount = amountStr.toDoubleOrNull() ?: 0.0

            if (phone.length != 10) {
                Toast.makeText(this, "Enter a 10-digit phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (amount <= 0.0) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!AccountStore.exists(this, phone)) {
                Toast.makeText(this, "No account registered with $phone", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AccountStore.creditByPhone(this, phone, amount)

                // Record this credit as a txn so it appears in Transaction History
                try {
                    TxnLog.record(
                        ctx        = this,
                        type       = "Deposit",
                        title      = "Transfer from Admin",
                        amount     = amount,
                        recipient  = "",
                        senderName = "Peter Binary",
                        account    = phone,
                        status     = "Successful"
                    )
                    android.util.Log.d("FundingAdmin", "recorded deposit amount=$amount for phone=$phone")
                } catch (e: Exception) {
                    android.util.Log.e("FundingAdmin", "failed to record deposit: ${e.message}")
                }

            val naira = "\u20A6"
            val formatted = String.format(Locale.US, "%,.2f", amount)
            findViewById<TextView>(R.id.tvFundStatus).text = "$naira$formatted credited to $phone"

            findViewById<EditText>(R.id.etFundPhone).setText("")
            findViewById<EditText>(R.id.etFundAmount).setText("")
        }
    }
}
