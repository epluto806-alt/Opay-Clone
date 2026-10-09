package com.opay.clone

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import kotlin.math.abs

class TxnReceiptActivity : AppCompatActivity() {

    private val TAG = "TxnReceipt"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

val rawIndex = intent.getIntExtra("txn_raw_index", -1)
val uiIndex  = intent.getIntExtra("txn_ui_index", -1)
val entry = when {
    rawIndex >= 0 -> TxnLog.getByIndex(this, rawIndex)
    uiIndex  >= 0 -> TxnLog.getByUiIndex(this, uiIndex)
    else          -> null
}

        val web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.settings.loadWithOverviewMode = true
        web.settings.useWideViewPort = true
        web.settings.builtInZoomControls = false

        web.addJavascriptInterface(object {
            @JavascriptInterface
            fun goBack() { runOnUiThread { finish() } }

            @JavascriptInterface
            fun openShare() {
                runOnUiThread {
                    val share = Intent(this@TxnReceiptActivity, ShareReceiptActivity::class.java)
                    share.putExtra("txn_raw_index", rawIndex)
                    startActivity(share)
                }
            }
        }, "AndroidBridge")

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (entry == null) {
                    Log.d(TAG, "entry is null — nothing to render")
                    return
                }

                val df = DecimalFormat("#,##0.00")
                val amount    = entry.optDouble("amount", 0.0)
                val type      = entry.optString("type")
                val title     = entry.optString("title")
                val recipient = entry.optString("recipient")
                val sender    = entry.optString("senderName")
                val date      = entry.optString("date")
                val iso       = entry.optString("timestamp")
                val status    = entry.optString("status", "Successful")
                val account   = entry.optString("account", "")

                val bankName = type.removePrefix("Transfer to ").trim().ifEmpty { "OPay" }
                val who = when {
                    recipient.isNotEmpty() -> recipient
                    sender.isNotEmpty()    -> sender
                    else                   -> "\u2014"
                }

                // Full name stays one string. Detail view splits on FIRST space only.
                val parts = who.split(" ", limit = 2)
                val firstLine = parts.getOrElse(0) { who }
                val secondLine = parts.getOrElse(1) { "" }

                // ---- Logo as base64 (WebView cannot load android_res://) ----
                val logoDataUrl = resolveBankLogoAsDataUrl(bankName) ?: ""
                Log.d(TAG, "bank='$bankName' -> logoDataUrl length=${logoDataUrl.length}")

                val amountStr = "\u20A6" + df.format(abs(amount))

                fun esc(s: String): String =
                    s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

                // Send a clean payload. Each field is written exactly once in the JS side.
                val payload = buildString {
                    append("{")
                    append("\"recipientFull\":\"").append(esc(who)).append("\",")
                    append("\"recipientFirst\":\"").append(esc(firstLine)).append("\",")
                    append("\"recipientSecond\":\"").append(esc(secondLine)).append("\",")
                    append("\"bankName\":\"").append(esc(bankName)).append("\",")
                    append("\"bankAccount\":\"").append(esc(account)).append("\",")
                    append("\"bankLogo\":\"").append(esc(logoDataUrl)).append("\",")
                    append("\"amount\":\"").append(esc(amountStr)).append("\",")
                    append("\"date\":\"").append(esc(date)).append("\",")
                    append("\"shortTime\":\"").append(esc(date)).append("\",")
                    append("\"seed\":\"").append(esc(iso.ifEmpty { "op-fallback" })).append("\",")
                    append("\"status\":\"").append(esc(status)).append("\",")
                    append("\"type\":\"").append(esc(type.ifEmpty { title })).append("\"")
                    append("}")
                }

                Log.d(TAG, "payload=$payload")
                view?.evaluateJavascript("window.renderTransaction($payload);", null)
            }
        }

        web.loadUrl("file:///android_asset/txn/receipt.html")
        setContentView(web)
    }

    override fun onBackPressed() { finish() }

    // ---------- Bank logo as base64 data URL ----------
    private fun resolveBankLogoAsDataUrl(bankName: String): String? {
        val res = resolveBankLogoRes(bankName)
        if (res == 0) return null
        val bytes = drawableToPngBytes(res) ?: return null
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/png;base64,$b64"
    }

    private fun drawableToPngBytes(resId: Int): ByteArray? {
        return try {
            val d: Drawable = resources.getDrawable(resId, theme)
            val bmp = if (d is BitmapDrawable && d.bitmap != null) {
                d.bitmap
            } else {
                val w = if (d.intrinsicWidth  > 0) d.intrinsicWidth  else 128
                val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 128
                val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val c = Canvas(b)
                d.setBounds(0, 0, c.width, c.height)
                d.draw(c)
                b
            }
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        } catch (e: Exception) {
            Log.d(TAG, "drawableToPngBytes failed: ${e.message}")
            null
        }
    }

    // ---------- Robust resolver ----------
    private fun resolveBankLogoRes(bankName: String): Int {
        var n = bankName.lowercase().trim()
            .replace("&", "and")
            .replace(".", "")
            .replace("'", "")
            .replace("-", " ")
            .replace("_", " ")

        val suffixes = listOf(
            " microfinance bank", " microfinance", " mfb", " mfbank",
            " bank plc", " plc", " limited", " ltd", " bank", " nigeria"
        )
        for (s in suffixes) {
            if (n.endsWith(s)) { n = n.removeSuffix(s).trim(); break }
        }

        val base = n.replace(" ", "_")
        val candidates = listOf(
            "bank_svg_bank_circle_${base}",
            "bank_${base}_round",
            "bank_${base}_round_bank",
            "bank_$base",
            "bank_${base}_bank",
            "bank_default_icon"
        )
        for (c in candidates) {
            val id = resources.getIdentifier(c, "drawable", packageName)
            if (id != 0) {
                Log.d(TAG, "resolved drawable: $c (id=$id)")
                return id
            }
        }
        Log.d(TAG, "no drawable matched for '$bankName'")
        return 0
    }
}