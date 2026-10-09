package com.opay.clone

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class ToBankActivity : AppCompatActivity() {

    private val bankPickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val name = result.data?.getStringExtra(ChooseBankActivity.EXTRA_BANK_NAME)
            val code = result.data?.getStringExtra(ChooseBankActivity.EXTRA_BANK_CODE)
            if (name != null && code != null) {
                selectedBankName = name
                selectedBankCode = code
                tvBankName.text = name
                tvBankName.setTextColor(0xFF0A0A0A.toInt())
                updateNextState()
                maybeResolve()
            }
        }
    }


    private lateinit var etAccount: EditText
    private lateinit var tvBankName: TextView
    private lateinit var tvResolvedName: TextView
    private var resolvedNameFinal: String? = null
    private lateinit var btnNext: TextView
    private var selectedBankName: String? = null
    private var selectedBankCode: String? = null

    private lateinit var vpToBank: ViewPager2
    private lateinit var dotsToBank: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private var currentSlide = 0

    private val bannerImages = listOf(
        R.drawable.opay_banner_1,
        R.drawable.opay_banner_2,
        R.drawable.opay_tobank_banner
    )

    private val autoSlide = object : Runnable {
        override fun run() {
            if (bannerImages.isNotEmpty()) {
                currentSlide = (currentSlide + 1) % bannerImages.size
                vpToBank.setCurrentItem(currentSlide, true)
                handler.postDelayed(this, 4000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_to_bank)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvHistory).setOnClickListener {
            startActivity(android.content.Intent(this, TransactionHistoryActivity::class.java))
        }

        etAccount = findViewById(R.id.etAccountNo)
        tvBankName = findViewById(R.id.tvBankName)
        tvResolvedName = findViewById(R.id.tvResolvedName)
        btnNext = findViewById(R.id.btnNextBank)

        vpToBank = findViewById(R.id.vpToBank)
        dotsToBank = findViewById(R.id.dotsToBank)
        vpToBank.adapter = BannerAdapter(bannerImages)
        setupDots()
        vpToBank.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentSlide = position
                updateDots(position)
            }
        })

        updateNextState()

        etAccount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                updateNextState()
                maybeResolve()
                val acct = etAccount.text.toString().trim()
                if (acct.length == 10 && selectedBankCode == null) {
                    suggestBanksFor(acct)
                } else {
                    val container = findViewById<LinearLayout>(R.id.llBankSuggestions)
                    if (container != null) container.visibility = View.GONE
                    val mr = findViewById<LinearLayout>(R.id.llMatchingBank)
                    if (mr != null) mr.visibility = View.GONE
                    val rr = findViewById<android.widget.ImageView>(R.id.ivMatchingRing)
                    rr?.clearAnimation()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<LinearLayout>(R.id.rowSelectBank).setOnClickListener {
            bankPickerLauncher.launch(android.content.Intent(this, ChooseBankActivity::class.java))
        }

        btnNext.setOnClickListener {
            if (selectedBankName == null || etAccount.text.length != 10) {
                Toast.makeText(this, "Select bank and enter 10-digit account number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Resolver-authoritative: never trust the TextView.
            // If resolution hasn't completed, block the tap and warn.
            val resolvedName = resolvedNameFinal
            if (resolvedName.isNullOrBlank()) {
                Toast.makeText(this, "Wait for account name to resolve", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            RecentsStore.add(this, RecentRecipient(
                name = resolvedName,
                account = etAccount.text.toString().trim(),
                bankName = selectedBankName ?: "",
                bankCode = selectedBankCode ?: "",
                isOpay = (selectedBankCode == "999992"),
                timestamp = System.currentTimeMillis()
            ))
            renderRecents()

            // Stage 2: bank amount screen
            val i = android.content.Intent(this, TransferToBankAmountActivity::class.java)
            i.putExtra("extra_account", etAccount.text.toString().trim())
            i.putExtra("extra_bank_code", selectedBankCode ?: "")
            i.putExtra("extra_bank_name", selectedBankName ?: "")
            i.putExtra("extra_resolved_name", resolvedName)
            startActivity(i)
        }
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
        dotsToBank.removeAllViews()
        val size = (6 * resources.displayMetrics.density).toInt()
        for (i in bannerImages.indices) {
            val dot = View(this)
            val lp = LinearLayout.LayoutParams(size, size)
            lp.setMargins(6, 0, 6, 0)
            dot.layoutParams = lp
            dot.setBackgroundResource(R.drawable.dot_indicator)
            dotsToBank.addView(dot)
        }
        updateDots(0)
    }

    private fun updateDots(active: Int) {
        for (i in 0 until dotsToBank.childCount) {
            dotsToBank.getChildAt(i).alpha = if (i == active) 1.0f else 0.35f
        }
    }

    private fun updateNextState() {
        val canProceed = etAccount.text.length == 10 && selectedBankName != null
        if (canProceed) {
            btnNext.setBackgroundResource(R.drawable.bg_next_enabled)
        } else {
            btnNext.setBackgroundResource(R.drawable.bg_next_disabled)
        }
    }

    private fun maybeResolve() {
        val acct = etAccount.text.toString().trim()
        val code = selectedBankCode
        if (acct.length != 10 || code.isNullOrEmpty()) {
            tvResolvedName.visibility = View.GONE
            return
        }
        tvResolvedName.visibility = View.VISIBLE
        tvResolvedName.text = "Resolving..."
        tvResolvedName.setTextColor(0xFF8E8E8E.toInt())
        resolvedNameFinal = null
        BankResolver.resolve(acct, code) { name ->
            runOnUiThread {
                if (name.isNullOrBlank()) {
                    tvResolvedName.text = "Could not resolve account name"
                    tvResolvedName.setTextColor(0xFFE53935.toInt())
                    resolvedNameFinal = null
                } else {
                    tvResolvedName.text = name
                    tvResolvedName.setTextColor(0xFF00B876.toInt())
                    resolvedNameFinal = name
                }
            }
        }
    }

    private fun renderRecents() {
        val container = findViewById<LinearLayout>(R.id.containerRecents) ?: return
        container.removeAllViews()

        val recents = RecentsStore.all(this)
        if (recents.isEmpty()) {
            val empty = TextView(this)
            empty.text = "No recent recipients yet"
            empty.setTextColor(0xFF8E8E8E.toInt())
            empty.textSize = 13f
            empty.gravity = android.view.Gravity.CENTER
            empty.setPadding(0, 32, 0, 32)
            container.addView(empty)
            return
        }

        recents.take(5).forEachIndexed { idx, r ->
            val row = layoutInflater.inflate(R.layout.item_recent_row, container, false)

            val nameTv = row.findViewById<TextView>(R.id.tvRecentName)
            val metaTv = row.findViewById<TextView>(R.id.tvRecentMeta)
            val badge = row.findViewById<ImageView>(R.id.ivRecentBadge)

            nameTv.text = r.name
            metaTv.text = "${r.account} ${r.bankName}"

            if (r.isOpay) {
                badge.setImageResource(R.drawable.bank_opay_round)
            } else {
                // Try to find matching bank logo
                val bank = BankList.all.firstOrNull { it.code == r.bankCode }
                if (bank != null) {
                    badge.setImageResource(bank.logoRes)
                } else {
                    badge.setImageResource(R.drawable.bank_default_icon)
                }
            }

            row.setOnClickListener {
                etAccount.setText(r.account)
                selectedBankName = r.bankName
                selectedBankCode = r.bankCode
                tvBankName.text = r.bankName
                tvBankName.setTextColor(0xFF0A0A0A.toInt())
                updateNextState()
                maybeResolve()
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

    // ---- Parallel bank suggestion (Option A) ----
    private val suggestionShortlist = listOf(
        "999992" to "OPay",
        "50515"  to "Moniepoint MFB",
        "999991" to "PalmPay",
        "044"    to "Access Bank",
        "058"    to "GTBank",
        "011"    to "First Bank",
        "033"    to "UBA",
        "057"    to "Zenith Bank",
        "50211"  to "Kuda",
        "070"    to "Fidelity Bank",
        "032"    to "Union Bank",
        "232"    to "Sterling Bank"
    )

    private var suggestPending = false

    private fun suggestBanksFor(account: String) {
        if (account.length != 10) return
        if (selectedBankCode != null) return

        val container = findViewById<LinearLayout>(R.id.llBankSuggestions) ?: return
        container.removeAllViews()
        container.visibility = View.GONE

        val matchingRow = findViewById<LinearLayout>(R.id.llMatchingBank)
        val ring = findViewById<android.widget.ImageView>(R.id.ivMatchingRing)
        matchingRow?.visibility = View.VISIBLE
        ring?.let { r ->
            val spin = android.view.animation.RotateAnimation(
                0f, 360f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 900
                repeatCount = android.view.animation.Animation.INFINITE
                interpolator = android.view.animation.LinearInterpolator()
            }
            r.startAnimation(spin)
        }

        // Order shortlist: most common banks first — this cuts the number of
        // sequential resolve calls to 1-2 in the majority of cases.
        val prioritized = suggestionShortlist.sortedBy { (code, _) ->
            when (code) {
                "50515"  -> 0  // Moniepoint
                "999991" -> 1  // PalmPay
                "999992" -> 2  // OPay
                "044"    -> 3  // Access
                "058"    -> 4  // GTBank
                "011"    -> 5  // First Bank
                "033"    -> 6  // UBA
                "057"    -> 7  // Zenith
                "50211"  -> 8  // Kuda
                "070"    -> 9  // Fidelity
                "032"    -> 10 // Union
                "232"    -> 11 // Sterling
                else     -> 99
            }
        }

        val matches = mutableListOf<Pair<String, String>>()

        fun tryNext(index: Int) {
            if (index >= prioritized.size) {
                runOnUiThread {
                    val mr = findViewById<LinearLayout>(R.id.llMatchingBank)
                    mr?.visibility = View.GONE
                    val rr = findViewById<android.widget.ImageView>(R.id.ivMatchingRing)
                    rr?.clearAnimation()
                    if (matches.isNotEmpty()) renderBankSuggestions(container, matches)
                }
                return
            }

            val (code, name) = prioritized[index]
            BankResolver.resolve(account, code) { resolved ->
                if (!resolved.isNullOrBlank()) {
                    matches.add(code to name)
                }
                // Continue sequentially
                tryNext(index + 1)
            }
        }

        tryNext(0)
    }

    private fun renderBankSuggestions(container: LinearLayout, matches: List<Pair<String, String>>) {
        container.removeAllViews()
        for ((code, name) in matches) {
            val row = layoutInflater.inflate(R.layout.item_bank_suggestion, container, false)
            val nameTv = row.findViewById<TextView>(R.id.tvSuggestionName)
            val badge = row.findViewById<ImageView>(R.id.ivSuggestionLogo)

            nameTv.text = name
            val bank = BankList.all.firstOrNull { it.code == code }
            badge.setImageResource(bank?.logoRes ?: R.drawable.bank_default_icon)

            row.setOnClickListener {
                selectedBankCode = code
                selectedBankName = name
                tvBankName.text = name
                tvBankName.setTextColor(0xFF0A0A0A.toInt())
                container.visibility = View.GONE
                updateNextState()
                maybeResolve()
            }
            container.addView(row)
        }
        container.visibility = View.VISIBLE
    }
}