package com.opay.clone

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MeScreenActivity : AppCompatActivity() {

    private var hidden = false
    private var csTapCount = 0
    private var csFirstTapTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_me_screen)

        // Security Center -> Set Payment PIN
        findViewById<android.view.View>(R.id.tileSecurityCenter)?.setOnClickListener {
            startActivity(android.content.Intent(this, SetPaymentPinActivity::class.java))
        }

        // Customer Service row -> Buy Token screen
        findViewById<android.view.View>(R.id.tileCustomerService)?.setOnClickListener {
            startActivity(android.content.Intent(this, BuyTokenActivity::class.java))
        }


        findViewById<android.widget.ImageView>(R.id.btnMeSettings).setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
        }
        hidden = AppPrefs.isBalanceHidden(this)

        refreshUI()

        findViewById<ImageView>(R.id.btnMeEye)?.setOnClickListener {
            hidden = !hidden
            AppPrefs.setBalanceHidden(this, hidden)
            applyHiddenState()
        }

        findViewById<View>(R.id.tabMeHome)?.setOnClickListener {
            finish()
        }

        wireCustomerServiceTap()

        findViewById<android.view.View>(R.id.rowTxnHistory)?.setOnClickListener {
            startActivity(android.content.Intent(this, TransactionHistoryActivity::class.java))
        }

        findViewById<android.view.View>(R.id.rowAccountLimits)?.setOnClickListener {
            startActivity(android.content.Intent(this, ReduceBalanceActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        hidden = AppPrefs.isBalanceHidden(this)
        applyHiddenState()
        refreshUI()
    }

    private fun wireCustomerServiceTap() {
        findViewById<android.view.View>(R.id.tileOpayUssd)?.setOnClickListener {
            val now = android.os.SystemClock.elapsedRealtime()
            if (csTapCount == 0 || now - csFirstTapTime > 5000) {
                csTapCount = 0
                csFirstTapTime = now
            }
            csTapCount++
            if (csTapCount >= 7) {
                csTapCount = 0
                showAdminCodeDialog()
            }
        }
    }

    private fun showAdminCodeDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Enter access code"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            setPadding(40, 30, 40, 30)
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Admin Access")
            .setMessage("Enter the secret code to continue")
            .setView(input)
            .setPositiveButton("OK", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val entered = input.text.toString().trim()
                if (entered == "*#*#OPAY080#*#*") {
                    dialog.dismiss()
                    startActivity(android.content.Intent(this, FundingAdminActivity::class.java))
                } else {
                    input.error = "Wrong code"
                }
            }
        }
        dialog.show()
    }

    private fun refreshUI() {
        val phone = AccountStore.getCurrentPhone(this)
        val acc = if (phone.isNotEmpty()) AccountStore.getAccount(this, phone) else null

        // Name: prefer AccountStore, fallback to AppPrefs
        val name = (acc?.name ?: AppPrefs.getName(this)).trim()
        val first = Safe.splitName(name).first
        findViewById<TextView>(R.id.tvMeName).text =
            if (first.isEmpty()) "Hi," else "Hi, $first"

        // Balance: prefer AccountStore (live), fallback to AppPrefs
        val rawBal = acc?.balance ?: AppPrefs.getBalance(this)
        val d = rawBal.replace(",", "").trim().toDoubleOrNull() ?: 0.0
        val naira = "\u20A6"
        findViewById<TextView>(R.id.tvMeBalance).text = naira + String.format("%,.2f", d)
        findViewById<TextView>(R.id.tvMeBalanceMasked).text = "\u2022\u2022\u2022\u2022"

        applyHiddenState()
    }

    private fun applyHiddenState() {
        val vis = findViewById<TextView>(R.id.tvMeBalance)
        val mask = findViewById<TextView>(R.id.tvMeBalanceMasked)
        val eye = findViewById<ImageView>(R.id.btnMeEye)

        if (hidden) {
            vis.visibility = View.GONE
            mask.visibility = View.VISIBLE
            eye.setImageResource(R.drawable.opay_me_eye_hide)
        } else {
            vis.visibility = View.VISIBLE
            mask.visibility = View.GONE
            eye.setImageResource(R.drawable.opay_me_eye_show)
        }
    }
}


