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

class TransferToOpayAmountActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAME = "recipient_name"
        const val EXTRA_ACCOUNT = "recipient_account"
        const val EXTRA_BANK = "recipient_bank"
        const val EXTRA_BANK_CODE = "recipient_bank_code"
    }

    private lateinit var etAmount: EditText
    private lateinit var etRemark: EditText
    private lateinit var btnConfirm: TextView

    private var recipientName: String = ""
    private var recipientAccount: String = ""
    private var recipientBank: String = ""
    private var recipientBankCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_to_opay_amount)

        recipientName = intent.getStringExtra(EXTRA_NAME) ?: ""
        recipientAccount = intent.getStringExtra(EXTRA_ACCOUNT) ?: ""
        recipientBank = intent.getStringExtra(EXTRA_BANK) ?: "OPay"
        recipientBankCode = intent.getStringExtra(EXTRA_BANK_CODE) ?: "999992"

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvRecipientName).text = recipientName
        findViewById<TextView>(R.id.tvRecipientAccount).text = formatAccount(recipientAccount)

        etAmount = findViewById(R.id.etAmount)
        etRemark = findViewById(R.id.etRemark)
        btnConfirm = findViewById(R.id.btnConfirmAmount)

        // Quick amount chips
        findViewById<TextView>(R.id.chip500).setOnClickListener { setAmount("500") }
        findViewById<TextView>(R.id.chip1000).setOnClickListener { setAmount("1000") }
        findViewById<TextView>(R.id.chip2000).setOnClickListener { setAmount("2000") }
        findViewById<TextView>(R.id.chip5000).setOnClickListener { setAmount("5000") }
        findViewById<TextView>(R.id.chip9999).setOnClickListener { setAmount("9999") }
        findViewById<TextView>(R.id.chip10000).setOnClickListener { setAmount("10000") }

        // Remark chips
        findViewById<TextView>(R.id.chipPurchase).setOnClickListener { etRemark.setText("Purchase") }
        findViewById<TextView>(R.id.chipPersonal).setOnClickListener { etRemark.setText("Personal") }

        etAmount.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true

                val raw = s?.toString()?.replace(",", "") ?: ""
                if (raw.isNotEmpty()) {
                    val formatted = formatWithCommas(raw)
                    if (formatted != s.toString()) {
                        etAmount.setText(formatted)
                        etAmount.setSelection(etAmount.text.length)
                    }
                }
                updateConfirmState()
                isFormatting = false
            }
        })

        btnConfirm.setOnClickListener {
            val v = etAmount.text.toString().replace(",", "").trim().toDoubleOrNull() ?: 0.0
            if (v < 10.0) {
                Toast.makeText(this, "Minimum is \u20A610", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (v > 5_000_000.0) {
                Toast.makeText(this, "Maximum is \u20A65,000,000", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showSpinnerThen(5000L) {
                showReminderPopup(v)
            }
        }

        updateConfirmState()
    }

    private fun setAmount(value: String) {
        val formatted = formatWithCommas(value)
        etAmount.setText(formatted)
        etAmount.setSelection(formatted.length)
        updateConfirmState()
    }

    private fun formatWithCommas(raw: String): String {
        if (raw.isEmpty()) return ""
        val dotIdx = raw.indexOf('.')
        val intPart = if (dotIdx >= 0) raw.substring(0, dotIdx) else raw
        val decPart = if (dotIdx >= 0) raw.substring(dotIdx) else ""
        val digitsOnly = intPart.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return decPart
        val sb = StringBuilder()
        var count = 0
        for (i in digitsOnly.length - 1 downTo 0) {
            sb.append(digitsOnly[i])
            count++
            if (count % 3 == 0 && i != 0) sb.append(',')
        }
        return sb.reverse().toString() + decPart
    }

    private fun updateConfirmState() {
        val amt = etAmount.text.toString().replace(",", "").trim().toDoubleOrNull() ?: 0.0
        val valid = amt >= 10.0 && amt <= 5_000_000.0
        btnConfirm.isEnabled = valid
        btnConfirm.alpha = if (valid) 1f else 0.4f

        val pill = findViewById<TextView>(R.id.tvTierPill)
        val label = tierLabel(amt)
        if (label.isEmpty()) {
            pill.visibility = View.GONE
        } else {
            pill.visibility = View.VISIBLE
            pill.text = label
        }
    }

    private fun formatAccount(acct: String): String {
        val clean = acct.replace(" ", "").trim()
        if (clean.length != 10) return clean
        return "${clean.substring(0,3)} ${clean.substring(3,6)} ${clean.substring(6)}"
    }
    private fun showSpinnerThen(delayMs: Long, onDone: () -> Unit) {
        val root = findViewById<android.view.ViewGroup>(android.R.id.content)
        val overlay = layoutInflater.inflate(R.layout.overlay_loading_spinner, root, false)
        root.addView(overlay)
        overlay.alpha = 0f
        overlay.animate().alpha(1f).setDuration(200).start()
        overlay.postDelayed({
            overlay.animate().alpha(0f).setDuration(200).withEndAction {
                root.removeView(overlay)
                onDone()
            }.start()
        }, delayMs)
    }

    private fun showReminderPopup(amount: Double) {
        val account     = intent.getStringExtra(EXTRA_ACCOUNT)      ?: ""
        val bankCode    = intent.getStringExtra(EXTRA_BANK_CODE)    ?: ""
        val bankName    = intent.getStringExtra(EXTRA_BANK)         ?: "OPay"
        val resolvedName= intent.getStringExtra(EXTRA_NAME)         ?: ""

        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val view = layoutInflater.inflate(R.layout.dialog_transfer_reminder, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        val df = java.text.DecimalFormat("#,##0.00")
        val amountText = "\u20A6" + df.format(amount)

        view.findViewById<android.widget.TextView>(R.id.tvReminderName).text = resolvedName
        view.findViewById<android.widget.TextView>(R.id.tvReminderAccount).text = formatAccount(account)
        view.findViewById<android.widget.TextView>(R.id.tvReminderBank).text = bankName
        view.findViewById<android.widget.TextView>(R.id.tvReminderAmount).text = amountText
        view.findViewById<android.widget.TextView>(R.id.tvReminderTier).text = tierLabel(amount)

        view.findViewById<android.widget.ImageView>(R.id.btnCloseReminder).setOnClickListener { dialog.dismiss() }
        view.findViewById<android.widget.TextView>(R.id.btnRecheck).setOnClickListener { dialog.dismiss() }
        view.findViewById<android.widget.TextView>(R.id.btnContinue).setOnClickListener {
            dialog.dismiss()
            showSpinnerThen(5000L) {
                val i = android.content.Intent(this, ConfirmPaymentActivity::class.java)
                i.putExtra("extra_account", account)
                i.putExtra("extra_bank_code", bankCode)
                i.putExtra("extra_bank_name", bankName)
                i.putExtra("extra_resolved_name", resolvedName)
                i.putExtra("extra_amount", amount)
                startActivity(i)
            }
        }

        dialog.show()
        dialog.window?.setGravity(android.view.Gravity.BOTTOM)
        val lp = dialog.window?.attributes
        if (lp != null) {
            lp.y = 0
            dialog.window?.attributes = lp
        }
    }

    private fun tierLabel(amount: Double): String {
        val v = amount
        return when {
            v >= 1_000_000_000.0 -> "Billions"
            v >=   100_000_000.0 -> "Hundreds of Millions"
            v >=    10_000_000.0 -> "Tens of Millions"
            v >=     1_000_000.0 -> "Millions"
            v >=       100_000.0 -> "Hundreds of Thousands"
            v >=        10_000.0 -> "Tens of Thousands"
            v >=         1_000.0 -> "Thousands"
            v >=           100.0 -> "Hundreds"
            else -> ""
        }
    }
}