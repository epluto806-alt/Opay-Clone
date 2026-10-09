package com.opay.clone

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class ConfirmPaymentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_payment)

        // Bottom-sheet look: translucent window, content anchored to bottom
        window.setBackgroundDrawableResource(android.R.color.transparent)
        val lp = window.attributes
        lp.gravity = android.view.Gravity.BOTTOM
        lp.dimAmount = 0.5f
        window.attributes = lp
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        // Close button
        findViewById<ImageView>(R.id.btnCloseConfirm).setOnClickListener { finish() }

        val account     = intent.getStringExtra("extra_account")       ?: ""
        val bankCode    = intent.getStringExtra("extra_bank_code")     ?: ""
        val bankName    = intent.getStringExtra("extra_bank_name")     ?: ""
        val resolvedName= intent.getStringExtra("extra_resolved_name") ?: ""
        val amount      = intent.getDoubleExtra("extra_amount", 0.0)

        val df = DecimalFormat("#,##0.00")
        val amountText = "\u20A6" + df.format(amount)

        // Read the current user's actual balance (same source as Home)
        val phone = com.opay.clone.AccountStore.getCurrentPhone(this)
        val acct = if (phone.isNotEmpty()) com.opay.clone.AccountStore.getAccount(this, phone) else null
        val balStr = acct?.balance ?: com.opay.clone.AppPrefs.getBalance(this)
        val balance = balStr.toDoubleOrNull() ?: 0.0
        val balanceText = "\u20A6" + df.format(balance)

        // Available Balance row
        findViewById<TextView>(R.id.tvAvailableBalance).text = "Available Balance (" + balanceText + ")"

        // Wallet row shows the same real balance
        findViewById<TextView>(R.id.tvWalletLabel).text = "Wallet (" + balanceText + ")"

        // Right-side of Wallet row: -amount if enough, Add Money if not
        val walletRight = findViewById<TextView>(R.id.tvWalletRight)
        if (balance >= amount) {
            walletRight.text = "-\u20A6" + df.format(amount)
            walletRight.setTextColor(0xFF0A0A0A.toInt())
        } else {
            walletRight.text = "Add Money >"
            walletRight.setTextColor(0xFF00B876.toInt())
        }

        // Insufficient vs Pay gate
        val insufficient = findViewById<TextView>(R.id.tvInsufficient)
        val hasEnough = balance >= amount && amount >= 10.0
        if (balance < amount) {
            insufficient.visibility = android.view.View.VISIBLE
        } else {
            insufficient.visibility = android.view.View.GONE
        }
        val tier = tierLabel(amount)

        // Big amount at top
        findViewById<TextView>(R.id.tvPayAmount).text = amountText

        // Bank row
        findViewById<TextView>(R.id.tvPayBank).text = bankName
        val logoRes = BankList.byCode(bankCode)?.logoRes ?: R.drawable.bank_default_icon
        findViewById<ImageView>(R.id.ivPayBankLogo).setImageResource(logoRes)

        // Account row
        findViewById<TextView>(R.id.tvPayAccount).text = formatAccount(account)

        // Name row
        findViewById<TextView>(R.id.tvPayName).text = resolvedName

        // Amount row + tier pill
        findViewById<TextView>(R.id.tvPayAmountRow).text = amountText
        findViewById<TextView>(R.id.tvPayTier).text = tier

        // Fee row
        findViewById<TextView>(R.id.tvPayFee).text = "\u20A60.00"



        // Pay — gated by balance; toasts Stage 5 when tapped and enabled
        val btnPay = findViewById<TextView>(R.id.btnPay)
        if (hasEnough) {
            btnPay.alpha = 1.0f
            btnPay.isEnabled = true
        } else {
            btnPay.alpha = 0.4f
            btnPay.isEnabled = false
        }
        btnPay.setOnClickListener {
            if (hasEnough) {
                PaymentPinDialog.show(this) {
                    // PIN correct — stay on this screen, show spinner overlay, then go to Processing
                    val rootView = findViewById<android.view.ViewGroup>(android.R.id.content)
                    val overlay = layoutInflater.inflate(R.layout.overlay_loading_spinner, rootView, false)
                    rootView.addView(overlay)

                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val i = android.content.Intent(this, TransferProcessingActivity::class.java)
                        i.putExtra("extra_account", account)
                        i.putExtra("extra_bank_code", bankCode)
                        i.putExtra("extra_bank_name", bankName)
                        i.putExtra("extra_resolved_name", resolvedName)
                        i.putExtra("extra_amount", amount)
                        startActivity(i)
                        finish()
                    }, 2000)
                }
            }
        }
    }

    private fun formatAccount(a: String): String {
        if (a.length != 10) return a
        return a.substring(0, 3) + " " + a.substring(3, 6) + " " + a.substring(6)
    }

    /**
     * Tier label — range-based, plural, capitalized first letter.
     * 100-999          Hundreds
     * 1,000-9,999      Thousands
     * 10,000-99,999    Tens of Thousands
     * 100,000-999,999  Hundreds of Thousands
     * 1,000,000-9,999,999      Millions
     * 10,000,000-99,999,999    Tens of Millions
     * 100,000,000-999,999,999  Hundreds of Millions
     * 1,000,000,000-9,999,999,999  Billions
     * 10,000,000,000-99,999,999,999 Tens of Billions
     * 100,000,000,000-999,999,999,999 Hundreds of Billions
     * >= 1,000,000,000,000 Trillions (and it keeps scaling, we add when needed)
     */
    private fun tierLabel(amount: Double): String {
        val v = amount
        return when {
            v >= 100_000_000_000_000_000.0 -> "Hundreds of Quadrillions"
            v >=  10_000_000_000_000_000.0 -> "Tens of Quadrillions"
            v >=   1_000_000_000_000_000.0 -> "Quadrillions"
            v >=     100_000_000_000_000.0 -> "Hundreds of Trillions"
            v >=      10_000_000_000_000.0 -> "Tens of Trillions"
            v >=       1_000_000_000_000.0 -> "Trillions"
            v >=         100_000_000_000.0 -> "Hundreds of Billions"
            v >=          10_000_000_000.0 -> "Tens of Billions"
            v >=           1_000_000_000.0 -> "Billions"
            v >=             100_000_000.0 -> "Hundreds of Millions"
            v >=              10_000_000.0 -> "Tens of Millions"
            v >=               1_000_000.0 -> "Millions"
            v >=                 100_000.0 -> "Hundreds of Thousands"
            v >=                  10_000.0 -> "Tens of Thousands"
            v >=                   1_000.0 -> "Thousands"
            v >=                     100.0 -> "Hundreds"
            else -> ""
        }
    }
}