package com.opay.clone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NotificationsActivity : AppCompatActivity() {

    private lateinit var tabTransactions: TextView
    private lateinit var tabServices: TextView
    private lateinit var tabActivities: TextView
    private lateinit var list: LinearLayout
    private lateinit var emptyState: LinearLayout
    private lateinit var scrollNotifications: View
    private val inflater by lazy { LayoutInflater.from(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        list = findViewById(R.id.notifList)
        emptyState = findViewById(R.id.emptyState)
        scrollNotifications = findViewById(R.id.scrollNotifications)
        tabTransactions = findViewById(R.id.tabTransactions)
        tabServices = findViewById(R.id.tabServices)
        tabActivities = findViewById(R.id.tabActivities)

        tabTransactions.setOnClickListener { showTransactions() }
        tabServices.setOnClickListener { showServices() }
        tabActivities.setOnClickListener { showActivities() }

        showTransactions()
    }

    private fun setTabActive(active: TextView, others: List<TextView>) {
        active.setBackgroundResource(R.drawable.bg_tab_selected)
        active.setTextColor(0xFF00B876.toInt())
        for (o in others) {
            o.setBackgroundResource(R.drawable.bg_tab_unselected)
            o.setTextColor(0xFF6A6A6A.toInt())
        }
    }

    private fun showTransactions() {
        setTabActive(tabTransactions, listOf(tabServices, tabActivities))
        list.removeAllViews()
        emptyState.visibility = View.GONE
        scrollNotifications.visibility = View.VISIBLE

        val items = listOf(
            Triple("OWealth Interest Earned", "Your Available Balance 'OWealth' interest earned ₦0.09. Your OWealth balance is ₦4.21.", "Today 09:43"),
            Triple("OWealth Interest Earned", "Your Available Balance 'OWealth' interest earned ₦0.09. Your OWealth balance is ₦4.12.", "Yesterday 09:56"),
            Triple("OWealth Interest Earned", "Your Available Balance 'OWealth' interest earned ₦0.09. Your OWealth balance is ₦4.03.", "Sep 26, 2026 11:14"),
            Triple("OWealth Interest Earned", "Your Available Balance 'OWealth' interest earned ₦0.09. Your OWealth balance is ₦3.94.", "Sep 25, 2026 11:08"),
            Triple("OWealth Interest Earned", "Your Available Balance 'OWealth' interest earned ₦0.09. Your OWealth balance is ₦3.85.", "Sep 24, 2026 11:02"),
            Triple("OWealth Interest Earned", "Your Available Balance 'OWealth' interest earned ₦0.09. Your OWealth balance is ₦3.76.", "Sep 23, 2026 10:55"),
            Triple("OWealth Interest Earned", "Your Available Balance 'OWealth' interest earned ₦0.09. Your OWealth balance is ₦3.67.", "Sep 22, 2026 10:47"),
            Triple("OWealth Interest Earned", "Your Available Balance 'OWealth' interest earned ₦0.09. Your OWealth balance is ₦3.58.", "Sep 21, 2026 10:32"),
            Triple("OWealth Interest Earned", "Your Available Balance 'OWealth' interest earned ₦0.09. Your OWealth balance is ₦3.49.", "Sep 20, 2026 10:21"),
            Triple("OWealth Interest Earned", "Your Available Balance 'OWealth' interest earned ₦0.09. Your OWealth balance is ₦3.40.", "Sep 19, 2026 10:14")
        )
        for ((title, body, time) in items) {
            val card = inflater.inflate(R.layout.item_notification_card, list, false)
            card.findViewById<TextView>(R.id.tvNotifTitle).text = title
            card.findViewById<TextView>(R.id.tvNotifBody).text = body
            card.findViewById<TextView>(R.id.tvNotifTime).text = time
            card.findViewById<ImageView>(R.id.ivExtra).visibility = View.GONE
            list.addView(card)
        }
    }

    private fun showActivities() {
        setTabActive(tabActivities, listOf(tabTransactions, tabServices))
        list.removeAllViews()
        emptyState.visibility = View.GONE
        scrollNotifications.visibility = View.VISIBLE

        val items = listOf(
            "Today 14:51", "Today 14:48", "Yesterday 14:48", "Yesterday 14:30",
            "Sep 26, 2026 14:15", "Sep 26, 2026 11:00", "Sep 25, 2026 14:12",
            "Sep 24, 2026 14:05", "Sep 23, 2026 13:40", "Sep 22, 2026 13:10"
        )
        for (time in items) {
            val card = inflater.inflate(R.layout.item_notification_card, list, false)
            card.findViewById<TextView>(R.id.tvNotifTitle).text = "Claim ₦5,600 from Transfers!"
            card.findViewById<TextView>(R.id.tvNotifBody).text = "Invite people you sent money to yesterday to join OPay and earn ₦5,600. Invite now!"
            card.findViewById<TextView>(R.id.tvNotifTime).text = time
            card.findViewById<ImageView>(R.id.ivExtra).visibility = View.VISIBLE
            card.findViewById<ImageView>(R.id.ivExtra).setImageResource(R.drawable.ic_money_bag)
            list.addView(card)
        }
    }

    private fun showServices() {
        setTabActive(tabServices, listOf(tabTransactions, tabActivities))
        list.removeAllViews()
        emptyState.visibility = View.VISIBLE
        scrollNotifications.visibility = View.GONE
    }
}

