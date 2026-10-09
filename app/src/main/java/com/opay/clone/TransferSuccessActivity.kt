package com.opay.clone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat
import java.util.Locale

class TransferSuccessActivity : AppCompatActivity() {

    private var txnIndex: Int = -1

    private val bannerImages = listOf(
        R.drawable.opay_banner_1,
        R.drawable.opay_banner_2,
        R.drawable.opay_tobank_banner
    )
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var currentSlide = 0
    private val autoSlide = object : Runnable {
        override fun run() {
            if (bannerImages.isNotEmpty()) {
                currentSlide = (currentSlide + 1) % bannerImages.size
                findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.vpPromo)?.setCurrentItem(currentSlide, true)
                handler.postDelayed(this, 4000)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(autoSlide, 4000)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(autoSlide)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("txn_index", txnIndex)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_success)

        // ---- Read transfer extras ----
        val xAccount  = intent.getStringExtra("extra_account")       ?: ""
        val xBankName = intent.getStringExtra("extra_bank_name")     ?: ""
        val xResolved = intent.getStringExtra("extra_resolved_name") ?: ""
        val xAmount   = intent.getDoubleExtra("extra_amount", 0.0)

        // ---- Record txn once ----
        if (savedInstanceState == null) {
            Log.d("TxnSuccess", "recording txn: account='$xAccount' bank='$xBankName' resolved='$xResolved' amount=$xAmount")
            val typeLabel = if (xBankName.isNotEmpty()) "Transfer to $xBankName" else "Transfer"
            txnIndex = TxnLog.record(
                ctx        = this,
                type       = typeLabel,
                title      = "Transfer successful",
                amount     = xAmount,
                recipient  = if (xResolved.isNotEmpty()) xResolved else xAccount,
                senderName = "",
                account    = xAccount,
                status     = "Successful"
            )

            
// Deduct amount + ₦50 stamp duty from the current user's balance
            
val phone = AccountStore.getCurrentPhone(this)
            
if (phone.isNotEmpty() && xAmount > 0.0) {
            
    val acct = AccountStore.getAccount(this, phone)
            
    val currentBal = acct?.balance?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            
    val newBal = (currentBal - xAmount - 50.0).coerceAtLeast(0.0)
            
    AccountStore.setBalance(this, phone, String.format(Locale.US, "%.2f", newBal))
            
    android.util.Log.d("TxnSuccess", "balance updated: $currentBal - $xAmount - 50.0 = $newBal")
            
}
        } else {
            txnIndex = savedInstanceState.getInt("txn_index", -1)
        }

        // ---- Promo slider ----
        val vp = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.vpPromo)
        vp?.adapter = BannerAdapter(bannerImages)
        vp?.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentSlide = position
                updateDots(position)
            }
        })
        setupDots()

        // ---- Amount ----
        val df = DecimalFormat("#,##0.00")
        findViewById<TextView>(R.id.tvSuccessAmount).text = "\u20A6" + df.format(xAmount)

        // ---- View Details -> native receipt ----
        findViewById<android.view.View>(R.id.rowViewDetails)?.setOnClickListener {
            if (txnIndex >= 0) {
                val r = Intent(this, TxnReceiptActivity::class.java)
                r.putExtra("txn_raw_index", txnIndex)
                startActivity(r)
            }
        }

        // ---- Done ----
        findViewById<TextView>(R.id.btnDone).setOnClickListener {
            val i = Intent(this, HomeActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
            finish()
        }
    }

    private fun setupDots() {
        val dotsLayout = findViewById<android.widget.LinearLayout>(R.id.dotsPromo) ?: return
        dotsLayout.removeAllViews()
        val size = (6 * resources.displayMetrics.density).toInt()
        for (i in bannerImages.indices) {
            val dot = android.view.View(this)
            val lp = android.widget.LinearLayout.LayoutParams(size, size)
            lp.setMargins(6, 0, 6, 0)
            dot.layoutParams = lp
            dot.setBackgroundResource(R.drawable.dot_indicator)
            dotsLayout.addView(dot)
        }
        updateDots(0)
    }

    private fun updateDots(active: Int) {
        val dotsLayout = findViewById<android.widget.LinearLayout>(R.id.dotsPromo) ?: return
        for (i in 0 until dotsLayout.childCount) {
            dotsLayout.getChildAt(i).alpha = if (i == active) 1.0f else 0.35f
        }
    }

    inner class BannerAdapter(private val images: List<Int>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<BannerAdapter.VH>() {
        inner class VH(v: android.widget.ImageView) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(v)
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
            val iv = android.widget.ImageView(parent.context)
            iv.layoutParams = androidx.recyclerview.widget.RecyclerView.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            iv.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            return VH(iv)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            (holder.itemView as android.widget.ImageView).setImageResource(images[position])
        }
        override fun getItemCount(): Int = images.size
    }
}