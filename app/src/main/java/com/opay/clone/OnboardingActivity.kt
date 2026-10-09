package com.opay.clone

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private lateinit var dotsLayout: LinearLayout
    private lateinit var dotViews: Array<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val pages = listOf(
            OnboardingPage(
                "We are Beyond Banking",
                "OPay is a one-stop mobile-based platform for payment, transportation, food & grocery and other important services in your everyday life.",
                R.drawable.omain_app_guide_01
            ),
            OnboardingPage(
                "Instant & Free Transfers",
                "Enjoy OPay to OPay transfer & unlimited FREE transfer to other banks every Friday",
                R.drawable.omain_app_guide_02
            ),
            OnboardingPage(
                "Airtime/Data Top-up with Cashback",
                "Get instant cashbacks on Data/Airtime recharge",
                R.drawable.omain_app_guide_03
            ),
            OnboardingPage(
                "OWealth - Balance with Interest",
                "OWealth: Use as balance with amazing daily interest",
                R.drawable.omain_app_guide_04
            ),
            OnboardingPage(
                "Reliable Debit Card",
                "Zero ATM withdrawals & maintenance fee\nAccepted at any POS, ATM & online channels",
                R.drawable.omain_app_guide_05
            )
        )

        val vp = findViewById<ViewPager2>(R.id.vpOnboarding)
        vp.adapter = OnboardingAdapter(pages)

        dotsLayout = findViewById(R.id.dotsLayout)
        setupDots(pages.size)
        setActiveDot(0)

        vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setActiveDot(position)
            }
        })

        findViewById<TextView>(R.id.tvSkip).setOnClickListener {
            goToLogin()
        }

        findViewById<MaterialButton>(R.id.btnCreateAccount).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            goToLogin()
        }
    }

    private fun goToLogin() {
        val hasAccount = AppPrefs.getPhone(this).isNotEmpty()
        val target = if (hasAccount) FingerprintActivity::class.java else LoginActivity::class.java
        startActivity(Intent(this, target))
        finish()
    }

    private fun setupDots(count: Int) {
        dotViews = Array(count) { View(this) }
        val density = resources.displayMetrics.density
        for (i in 0 until count) {
            val dot = View(this)
            val size = (8 * density).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
            dot.layoutParams = params
            dot.setBackgroundResource(R.drawable.dot_indicator)
            dotViews[i] = dot
            dotsLayout.addView(dot)
        }
    }

    private fun setActiveDot(index: Int) {
        for (i in dotViews.indices) {
            dotViews[i].alpha = if (i == index) 1.0f else 0.35f
        }
    }
}

