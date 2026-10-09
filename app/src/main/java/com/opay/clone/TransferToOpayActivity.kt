package com.opay.clone

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class TransferToOpayActivity : AppCompatActivity() {

    private lateinit var vpPromo: ViewPager2
    private lateinit var dotsLayout: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var currentSlide = 0

    private lateinit var etRecipient: android.widget.EditText
    private lateinit var tvRecipientResolved: android.widget.TextView
    private var resolvedRecipientName: String? = null

    private val bannerImages = listOf(
        R.drawable.opay_banner_1,
        R.drawable.opay_banner_2,
        R.drawable.opay_tobank_banner
    )

    private val autoSlide = object : Runnable {
        override fun run() {
            if (bannerImages.isNotEmpty()) {
                currentSlide = (currentSlide + 1) % bannerImages.size
                vpPromo.setCurrentItem(currentSlide, true)
                handler.postDelayed(this, 4000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<android.widget.TextView>(R.id.tvHistory)?.setOnClickListener {
            startActivity(android.content.Intent(this, TransactionHistoryActivity::class.java))
        }
        findViewById<android.widget.TextView>(R.id.tabFavourites).isEnabled = false

        etRecipient = findViewById(R.id.etRecipient)
        tvRecipientResolved = findViewById(R.id.tvRecipientResolved)

        etRecipient.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { maybeResolveRecipient() }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        tvRecipientResolved.setOnClickListener {
            val name = resolvedRecipientName
            val acct = etRecipient.text.toString().trim()
            if (!name.isNullOrBlank() && acct.length == 10) {
                showTransferConfirmDialog(name, acct)
            }
        }

        vpPromo = findViewById(R.id.vpPromo)
        dotsLayout = findViewById(R.id.dotsPromo)

        vpPromo.adapter = BannerAdapter(bannerImages)
        setupDots()
        vpPromo.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentSlide = position
                updateDots(position)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(autoSlide, 4000)
        renderRecents()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(autoSlide)
    }

    private fun setupDots() {
        dotsLayout.removeAllViews()
        val size = (6 * resources.displayMetrics.density).toInt()
        for (i in bannerImages.indices) {
            val dot = View(this)
            val lp = LinearLayout.LayoutParams(size, size)
            lp.setMargins(6, 0, 6, 0)
            dot.layoutParams = lp
            dot.setBackgroundResource(R.drawable.dot_indicator)
            dotsLayout.addView(dot)
        }
        updateDots(0)
    }

    private fun updateDots(active: Int) {
        for (i in 0 until dotsLayout.childCount) {
            dotsLayout.getChildAt(i).alpha = if (i == active) 1.0f else 0.35f
        }
    }

    private fun showTransferConfirmDialog(name: String, acct: String) {
        val view = layoutInflater.inflate(R.layout.dialog_opay_transfer_confirm, null)

        view.findViewById<android.widget.TextView>(R.id.tvDialogName).text = name
        view.findViewById<android.widget.TextView>(R.id.tvDialogBank).text = "OPay"
        view.findViewById<android.widget.TextView>(R.id.tvDialogAccount).text = formatOpayAccount(acct)

        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        dialog.setCancelable(true)

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        val width = (resources.displayMetrics.widthPixels * 0.92).toInt()
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        view.findViewById<android.widget.ImageView>(R.id.btnCloseDialog).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<android.widget.TextView>(R.id.btnRecheck).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<android.widget.TextView>(R.id.btnDialogConfirm).setOnClickListener {
            dialog.dismiss()
            RecentsStore.add(this, RecentRecipient(
                name = name,
                account = acct,
                bankName = "OPay",
                bankCode = "999992",
                isOpay = true,
                timestamp = System.currentTimeMillis()
            ))
            renderRecents()
            val i = android.content.Intent(this, TransferToOpayAmountActivity::class.java)
            i.putExtra(TransferToOpayAmountActivity.EXTRA_NAME, name)
            i.putExtra(TransferToOpayAmountActivity.EXTRA_ACCOUNT, acct)
            i.putExtra(TransferToOpayAmountActivity.EXTRA_BANK, "OPay")
            i.putExtra(TransferToOpayAmountActivity.EXTRA_BANK_CODE, "999992")
            startActivity(i)
        }

        dialog.show()
    }

    private fun formatOpayAccount(acct: String): String {
        val clean = acct.replace(" ", "").trim()
        if (clean.length != 10) return clean
        return "${clean.substring(0,3)} ${clean.substring(3,6)} ${clean.substring(6)}"
    }

    private fun maybeResolveRecipient() {
        val acct = etRecipient.text.toString().trim()
        if (acct.length != 10 || !acct.all { it.isDigit() }) {
            tvRecipientResolved.visibility = View.GONE
            resolvedRecipientName = null
            return
        }
        tvRecipientResolved.visibility = View.VISIBLE
        tvRecipientResolved.text = "Resolving..."
        tvRecipientResolved.setTextColor(0xFF8E8E8E.toInt())
        BankResolver.resolve(acct, "999992") { name ->
            runOnUiThread {
                if (name.isNullOrBlank()) {
                    tvRecipientResolved.text = "Could not resolve OPay account"
                    tvRecipientResolved.setTextColor(0xFFE53935.toInt())
                    resolvedRecipientName = null
                } else {
                    tvRecipientResolved.text = name
                    tvRecipientResolved.setTextColor(0xFF00B876.toInt())
                    resolvedRecipientName = name
                }
            }
        }
    }

    private fun renderRecents() {
        val container = findViewById<LinearLayout>(R.id.containerRecents) ?: return
        container.removeAllViews()

        val recents = RecentsStore.all(this).filter { it.isOpay }
        if (recents.isEmpty()) {
            val empty = android.widget.TextView(this)
            empty.text = "No recent OPay recipients yet"
            empty.setTextColor(0xFF8E8E8E.toInt())
            empty.textSize = 13f
            empty.gravity = android.view.Gravity.CENTER
            empty.setPadding(0, 32, 0, 32)
            container.addView(empty)
            return
        }

        recents.take(5).forEach { r ->
            val row = layoutInflater.inflate(R.layout.item_recent_row, container, false)
            row.findViewById<android.widget.TextView>(R.id.tvRecentName).text = r.name
            row.findViewById<android.widget.TextView>(R.id.tvRecentMeta).text = "${r.account} OPay"
            row.findViewById<android.widget.ImageView>(R.id.ivRecentBadge).setImageResource(R.drawable.bank_opay_round)

            row.setOnClickListener {
                etRecipient.setText(r.account)
                maybeResolveRecipient()
            }

            container.addView(row)
        }
    }

    inner class BannerAdapter(private val images: List<Int>) : RecyclerView.Adapter<BannerAdapter.VH>() {
        inner class VH(v: ImageView) : RecyclerView.ViewHolder(v)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val iv = ImageView(parent.context)
            iv.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            return VH(iv)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            (holder.itemView as ImageView).setImageResource(images[position])
        }
        override fun getItemCount() = images.size
    }
}
