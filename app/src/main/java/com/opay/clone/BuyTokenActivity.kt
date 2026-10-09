package com.opay.clone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BuyTokenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy_token)

        val loading = findViewById<View>(R.id.btLoading)
        val menuOverlay = findViewById<View>(R.id.btMenuOverlay)

        // Show loading for 2 seconds
        loading.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            loading.visibility = View.GONE
        }, 2000)

        findViewById<ImageView>(R.id.btnBtBack).setOnClickListener { finish() }

        findViewById<ImageView>(R.id.btnBtMenu).setOnClickListener {
            menuOverlay.visibility = View.VISIBLE
        }
        findViewById<ImageView>(R.id.btnBtCloseMenu).setOnClickListener {
            menuOverlay.visibility = View.GONE
        }

        // Only "Buy Token" in menu is live  the other rows are dead
        // (The menu's Buy Token TextView is the 3rd child of the LinearLayout inside overlay)
        val menuRoot = menuOverlay as android.widget.FrameLayout
        val menuList = menuRoot.getChildAt(0) as LinearLayout
        // Children of menuList: [close X, Buy Token, About, Contact Us, How to Use]
        if (menuList.childCount >= 5) {
            menuList.getChildAt(1).setOnClickListener {
                menuOverlay.visibility = View.GONE
                openUrl("https://t.me/OnlyWebtools")
            }
        }

        // Seller 1 -> Telegram
        findViewById<LinearLayout>(R.id.rowSellerTaiwo).setOnClickListener {
            openUrl("https://t.me/Ciphercryptoo1")
        }

        // Seller 2 -> Telegram
        findViewById<LinearLayout>(R.id.rowSellerAsonex).setOnClickListener {
            openUrl("https://t.me/OnlyWebtools")
        }

        // Top-up Balance -> chooser dialog
        findViewById<View>(R.id.btnSentProof).setOnClickListener {
            val phone = AccountStore.getCurrentPhone(this)
            if (phone.isNotEmpty()) {
                AccountStore.setPending(this, phone, true)
            }
            val sellers = arrayOf("TAIWO", "ASONEX")
            android.app.AlertDialog.Builder(this)
                .setTitle("Top-up Balance")
                .setMessage("Choose a verified seller to contact")
                .setItems(sellers) { _, which ->
                    val url = if (which == 0) "https://t.me/Ciphercryptoo1" else "https://t.me/OnlyWebtools"
                    Toast.makeText(this, "Pending Deposit", Toast.LENGTH_LONG).show()
                    openUrl(url)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }
}
