package com.opay.clone

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class TransactionHistoryActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.settings.loadWithOverviewMode = true
        web.settings.useWideViewPort = true
        web.settings.builtInZoomControls = false

        web.webViewClient = WebViewClient()

        // JS bridge for receipt taps
        web.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun openReceipt(index: String) {
                runOnUiThread {
                    val i = android.content.Intent(this@TransactionHistoryActivity, TxnReceiptActivity::class.java)
                    i.putExtra("txn_raw_index", index.toIntOrNull() ?: -1)
                    startActivity(i)
                }
            }

            @android.webkit.JavascriptInterface
            fun goBack() {
                runOnUiThread { finish() }
            }
        }, "Android")

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                val json = TxnLog.all(this@TransactionHistoryActivity)
                val inject = "localStorage.setItem('transactionHistory', JSON.stringify($json)); if (window.renderHistory) window.renderHistory();"
                view?.evaluateJavascript(inject, null)
            }
        }

        web.loadUrl("file:///android_asset/txn/index.html")

        setContentView(web)
    }

    override fun onBackPressed() {
        finish()
    }
}