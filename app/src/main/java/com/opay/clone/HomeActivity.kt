package com.opay.clone

import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Looper
import android.os.Handler
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2


import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import android.os.Bundle
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager

class HomeActivity : AppCompatActivity() {

    

    private lateinit var vpVoucherHotDeal: ViewPager2
    private lateinit var dotsVoucherHotDeal: LinearLayout
    private val vhHandler = Handler(Looper.getMainLooper())
    private var vhSlide = 0

    private val voucherSlides: List<Triple<String, String, String>>
        get() = listOf(
            Triple(getString(R.string.slide_header_vouchers), getString(R.string.slide_title_vouchers), getString(R.string.slide_subtitle_vouchers)),
            Triple(getString(R.string.slide_header_hotdeal), getString(R.string.slide_title_hotdeal), getString(R.string.slide_subtitle_hotdeal))
        )
    private val voucherIcons = listOf(R.drawable.opay_banner_voucher, R.drawable.opay_banner_coin)

    private lateinit var vpTasks: ViewPager2
    private lateinit var dotsTasks: LinearLayout
    private var tasksSlide = 0
    private val tasksHandler = Handler(Looper.getMainLooper())

    private val tasksImages = listOf(
        R.drawable.home_tasks_slide1,
        R.drawable.home_tasks_slide2,
        R.drawable.home_tasks_slide3
    )

    private val tasksAutoSlide = object : Runnable {
        override fun run() {
            tasksSlide = (tasksSlide + 1) % tasksImages.size
            vpTasks.setCurrentItem(tasksSlide, true)
            tasksHandler.postDelayed(this, 4000)
        }
    }


    private val vhAutoSlide = object : Runnable {
        override fun run() {
            vhSlide = (vhSlide + 1) % voucherSlides.size
            vpVoucherHotDeal.setCurrentItem(vhSlide, true)
            vhHandler.postDelayed(this, 4000)
        }
    }

    private var tapCount = 0
    private var firstTapTime = 0L
    private var hidden = false

    private fun naira(): String = "\u20A6"
    private fun arrow(): String = "\u203A"

    private fun formatMoney(raw: String): String {
        val cleaned = raw.replace(",", "").trim()
        val d = cleaned.toDoubleOrNull() ?: return cleaned
        return String.format("%,.2f", d)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        hidden = AppPrefs.isBalanceHidden(this)

        // Savings Vouchers + Hot Deal slider
        vpVoucherHotDeal = findViewById(R.id.vpVoucherHotDeal)
        dotsVoucherHotDeal = findViewById(R.id.dotsVoucherHotDeal)
        vpVoucherHotDeal.adapter = VoucherHotDealAdapter() 
        setupVoucherDots()
        vpVoucherHotDeal.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                vhSlide = position
                updateVoucherDots(position)
            }
        })

        // Complete Tasks slider
        vpTasks = findViewById(R.id.vpTasks)
        dotsTasks = findViewById(R.id.dotsTasks)
        vpTasks.adapter = TasksAdapter()
        setupTasksDots()
        vpTasks.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tasksSlide = position
                updateTasksDots(position)
            }
        })

        refreshUI()

        // Pull-to-refresh
        findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.homeRefresh)?.setOnRefreshListener {
        refreshUI()
            findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.homeRefresh).isRefreshing = false
        }

        // Top bar
        findViewById<ImageView>(R.id.btnSupport)?.setOnClickListener {
            startActivity(Intent(this, CustomerServiceActivity::class.java))
        }
        findViewById<ImageView>(R.id.btnScan)?.setOnClickListener {
            startActivity(Intent(this, ScanQrActivity::class.java))
        }
        findViewById<ImageView>(R.id.btnBell)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        // Balance card
        findViewById<ImageView>(R.id.btnEye)?.setOnClickListener {
            hidden = !hidden
            AppPrefs.setBalanceHidden(this, hidden)
            val eyePhone = AccountStore.getCurrentPhone(this)
            val eyeAcct = if (eyePhone.isNotEmpty()) AccountStore.getAccount(this, eyePhone) else null
            val bal = eyeAcct?.balance ?: AppPrefs.getBalance(this)
            val display = findViewById<TextView>(R.id.tvBalance)
            val masked = findViewById<TextView>(R.id.tvBalanceMasked)
            if (hidden) {
                display.visibility = View.GONE
                masked.visibility = View.VISIBLE
            } else {
                display.visibility = View.VISIBLE
                masked.visibility = View.GONE
                display.text = naira() + formatMoney(bal) + " " + arrow()
            }
        }
        findViewById<TextView>(R.id.tvTxnHistory)?.setOnClickListener {
            startActivity(Intent(this, TransactionHistoryActivity::class.java))
        }

        // 7-tap admin on Add Money
        findViewById<TextView>(R.id.tvAddMoney)?.setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            if (tapCount == 0 || now - firstTapTime > 5000) {
                tapCount = 0
                firstTapTime = now
            }
            tapCount++
            if (tapCount >= 12) {
                tapCount = 0
                startActivity(Intent(this, AdminPanelActivity::class.java))
            }
        }

        // Action row
        findViewById<View>(R.id.tileToOpay)?.setOnClickListener {
            startActivity(Intent(this, TransferToOpayActivity::class.java))
        }
        findViewById<View>(R.id.tileToBank)?.setOnClickListener {
            startActivity(Intent(this, ToBankActivity::class.java))
        }
        findViewById<View>(R.id.tileWithdraw)?.setOnClickListener {
            startActivity(Intent(this, WithdrawActivity::class.java))
        }

        // Quick access

        // Bottom nav  Home tab
        findViewById<View>(R.id.tabHome)?.setOnClickListener {
            supportFragmentManager.popBackStack(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }

        // Bottom nav  Me tab
        findViewById<View>(R.id.tabMe)?.setOnClickListener {
            startActivity(Intent(this, MeScreenActivity::class.java))
        }
    }

    override fun onPause() {
        super.onPause()
        vhHandler.removeCallbacks(vhAutoSlide)
        tasksHandler.removeCallbacks(tasksAutoSlide)
    }

    override fun onResume() {
        super.onResume()
        try { populateRecentTxns() } catch (_: Exception) {}
        hidden = AppPrefs.isBalanceHidden(this)
        vhHandler.postDelayed(vhAutoSlide, 4000)
        tasksHandler.postDelayed(tasksAutoSlide, 4000)

        refreshUI()
    }

    private fun refreshUI() {
        try { populateRecentTxns() } catch (_: Exception) {}
        val phone = AccountStore.getCurrentPhone(this)
        val acct = if (phone.isNotEmpty()) AccountStore.getAccount(this, phone) else null

        val fullName = acct?.name?.takeIf { it.isNotEmpty() } ?: AppPrefs.getName(this)
        val balance = acct?.balance ?: AppPrefs.getBalance(this)

        val parts = fullName.trim().split(" ").filter { it.isNotEmpty() }
        val greeting = when {
            parts.size >= 3 -> parts[1]
            parts.size >= 1 -> parts[0]
            else -> ""
        }

        findViewById<TextView>(R.id.tvGreeting).text =
            if (greeting.isEmpty()) "Hi," else "Hi, $greeting"

        val display = findViewById<TextView>(R.id.tvBalance)
        display.text = naira() + formatMoney(balance)

        val masked = findViewById<TextView>(R.id.tvBalanceMasked)
        masked.text = "\u2022\u2022\u2022\u2022"

        if (hidden) {
            display.visibility = View.GONE
            masked.visibility = View.VISIBLE
        } else {
            display.visibility = View.VISIBLE
            masked.visibility = View.GONE
        }
    }

    private fun showAdminDialog() {
        val pad = (20 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        val nameInput = EditText(this).apply {
            hint = "Full name"
            setText("John Doe")
        }
        val balanceInput = EditText(this).apply {
            hint = "Balance (e.g. 5000000.00)"
            inputType = InputType.TYPE_CLASS_TEXT
            setText(formatMoney(AppPrefs.getBalance(this@HomeActivity)))
            filters = arrayOf(android.text.InputFilter { source, _, _, _, _, _ ->
                source.filter { it.isDigit() || it == ',' || it == '.' }.toString()
            })
        }

        layout.addView(TextView(this).apply { text = "Full Name"; textSize = 12f })
        layout.addView(nameInput)
        layout.addView(TextView(this).apply { text = "Balance"; textSize = 12f; setPadding(0, pad / 2, 0, 0) })
        layout.addView(balanceInput)

        AlertDialog.Builder(this)
            .setTitle("Admin")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                AppPrefs.setName(this, nameInput.text.toString())
                AppPrefs.setBalance(this, balanceInput.text.toString().replace(",", ""))
        refreshUI()
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupVoucherDots() {
        dotsVoucherHotDeal.removeAllViews()
        val size = (6 * resources.displayMetrics.density).toInt()
        for (i in voucherSlides.indices) {
            val dot = View(this)
            val lp = LinearLayout.LayoutParams(size, size)
            lp.setMargins(6, 0, 6, 0)
            dot.layoutParams = lp
            dot.setBackgroundResource(R.drawable.dot_indicator)
            dotsVoucherHotDeal.addView(dot)
        }
        updateVoucherDots(0)
    }

    private fun updateVoucherDots(active: Int) {
        for (i in 0 until dotsVoucherHotDeal.childCount) {
            dotsVoucherHotDeal.getChildAt(i).alpha = if (i == active) 1.0f else 0.35f
        }
    }

    inner class VoucherHotDealAdapter : RecyclerView.Adapter<VoucherHotDealAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_voucher_slide, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val slide = voucherSlides[position]
            holder.itemView.findViewById<TextView>(R.id.tvSlideHeader).text = slide.first
            holder.itemView.findViewById<TextView>(R.id.tvSlideTitle).text = slide.second
            holder.itemView.findViewById<TextView>(R.id.tvSlideSubtitle).text = slide.third
            holder.itemView.findViewById<ImageView>(R.id.ivSlideIcon).setImageResource(voucherIcons[position])
        }
        override fun getItemCount() = voucherSlides.size
    }

    private fun setupTasksDots() {
        dotsTasks.removeAllViews()
        val size = (6 * resources.displayMetrics.density).toInt()
        for (i in tasksImages.indices) {
            val dot = View(this)
            val lp = LinearLayout.LayoutParams(size, size)
            lp.setMargins(6, 0, 6, 0)
            dot.layoutParams = lp
            dot.setBackgroundResource(R.drawable.dot_indicator)
            dotsTasks.addView(dot)
        }
        updateTasksDots(0)
    }

    private fun updateTasksDots(active: Int) {
        for (i in 0 until dotsTasks.childCount) {
            dotsTasks.getChildAt(i).alpha = if (i == active) 1.0f else 0.35f
        }
    }

    inner class TasksAdapter : RecyclerView.Adapter<TasksAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tasks_slide, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            (holder.itemView as ImageView).setImageResource(tasksImages[position])
        }
        override fun getItemCount() = tasksImages.size
    }

    
    // ================= RECENT TRANSACTIONS (2 stacked rows) =================

    private fun populateRecentTxns() {
        val container = findViewById<LinearLayout>(R.id.llRecentTxns) ?: return
        container.removeAllViews()

        val entries: List<Pair<Int, org.json.JSONObject>> = try {
            val arr = org.json.JSONArray(TxnLog.all(this))
            val tmp = mutableListOf<Pair<Int, org.json.JSONObject>>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                tmp.add(i to o)
            }
            tmp.sortByDescending { it.second.optString("timestamp") }
            tmp.take(2)
        } catch (e: Exception) {
            android.util.Log.e("HomeRecent", "reload failed: ${e.message}")
            emptyList()
        }

        if (entries.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No transactions yet"
            tv.setTextColor(0xFF8E8E8E.toInt())
            tv.textSize = 11f
            tv.setPadding(0, 20, 0, 20)
            tv.gravity = android.view.Gravity.CENTER
            container.addView(tv)
            return
        }

        val df = java.text.DecimalFormat("#,##0.00")

        entries.forEach { (rawIndex, entry) ->
            val row = layoutInflater.inflate(R.layout.item_home_recent_txn, container, false)

            val icon  = row.findViewById<ImageView>(R.id.ivItemTxnIcon)
            val title = row.findViewById<TextView>(R.id.tvItemTxnTitle)
            val date  = row.findViewById<TextView>(R.id.tvItemTxnDate)
            val amt   = row.findViewById<TextView>(R.id.tvItemTxnAmount)
            val pill  = row.findViewById<TextView>(R.id.tvItemTxnStatus)

            val type = entry.optString("type")
            val isDeposit = type.equals("Deposit", ignoreCase = true)
            val who = if (isDeposit) entry.optString("senderName", "Admin")
                      else entry.optString("recipient", "Unknown")
            val prefix = if (isDeposit) "Transfer from " else "Transfer to "
            val amount = entry.optDouble("amount", 0.0)
            val dateStr = entry.optString("date")
            val status = entry.optString("status", "Successful")

            val amountText = (if (isDeposit) "+" else "-") + "\u20A6" + df.format(kotlin.math.abs(amount))

            icon.setImageResource(if (isDeposit) R.drawable.ic_txn_down else R.drawable.ic_txn_up)
            title.text = prefix + who
            date.text = dateStr
            amt.text = amountText
            amt.setTextColor(if (isDeposit) 0xFF00A86B.toInt() else 0xFF0A0A0A.toInt())
            pill.text = status

            row.setOnClickListener {
                try {
                    val intent = Intent(this, TxnReceiptActivity::class.java)
                    intent.putExtra("txn_raw_index", rawIndex)
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("HomeRecent", "tap failed: ${e.message}")
                }
            }

            container.addView(row)
        }
    }}