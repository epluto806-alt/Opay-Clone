package com.opay.clone

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class ShareReceiptActivity : AppCompatActivity() {

    private lateinit var card: View
    private var entry: JSONObject? = null
    private var rawIndex: Int = -1
    private val df = DecimalFormat("#,##0.00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_receipt)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        rawIndex = intent.getIntExtra("txn_raw_index", -1)
        entry = if (rawIndex >= 0) TxnLog.getByIndex(this, rawIndex) else null

        card = findViewById(R.id.cardWrap)

        bind()

        findViewById<LinearLayout>(R.id.btnShareImage).setOnClickListener { shareImage() }
        findViewById<LinearLayout>(R.id.btnSharePdf).setOnClickListener { sharePdf() }
    }

    private fun bind() {
        val e = entry ?: return
        val amount = e.optDouble("amount", 0.0)
        val recipient = e.optString("recipient").ifEmpty { e.optString("senderName") }
        val bankName = e.optString("type").removePrefix("Transfer to ").trim().ifEmpty { "OPay" }
        val account = e.optString("account", "")
        val status = e.optString("status", "Successful")
        val iso = e.optString("timestamp", "")
        val myPhone = AccountStore.getCurrentPhone(this)
        val myName = AccountStore.getAccount(this, myPhone)?.name ?: AppPrefs.getName(this)
        val maskedPhone = if (myPhone.length >= 10)
            myPhone.take(3) + "****" + myPhone.takeLast(3)
        else myPhone

        findViewById<TextView>(R.id.tvAmount).text = "\u20A6" + df.format(abs(amount))
        findViewById<TextView>(R.id.tvStatus).text = status
        findViewById<TextView>(R.id.tvDate).text = opayDateFromIso(iso)
        findViewById<TextView>(R.id.tvRecipientName).text = recipient.trim().uppercase().ifEmpty { "\u2014" }
        findViewById<TextView>(R.id.tvRecipientBank).text =
            bankName + if (account.isNotEmpty()) " | $account" else ""
        findViewById<TextView>(R.id.tvSenderName).text = myName.trim().uppercase().ifEmpty { "\u2014" }
        findViewById<TextView>(R.id.tvSenderBank).text = "OPay | $maskedPhone"

        val txnTv = findViewById<TextView>(R.id.tvTxnNo)
        val sesTv = findViewById<TextView>(R.id.tvSessionId)

        txnTv.text = stableDigits("$iso-txn", 24)
        sesTv.text = stableDigits("$iso-session", 31)

        listOf(txnTv, sesTv).forEach { tv ->
            tv.isSingleLine = true
            tv.ellipsize = null
        }
    }

    private fun opayDateFromIso(iso: String): String {
        val ts: Long = try {
            if (iso.isNotEmpty()) {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                sdf.parse(iso)?.time ?: System.currentTimeMillis()
            } else System.currentTimeMillis()
        } catch (_: Exception) { System.currentTimeMillis() }

        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val suffix = when {
            day in 11..13 -> "th"
            day % 10 == 1 -> "st"
            day % 10 == 2 -> "nd"
            day % 10 == 3 -> "rd"
            else -> "th"
        }
        val out = SimpleDateFormat("MMM d'${suffix}', yyyy HH:mm:ss", Locale.US)
        return out.format(Date(ts))
    }

    private fun stableDigits(seed: String, length: Int): String {
        var h = 0x811c9dc5.toInt()
        for (c in seed) { h = h xor c.code; h *= 0x01000193 }
        val sb = StringBuilder(length)
        var x = h
        for (i in 0 until length) {
            x = x xor (x shl 13); x = x xor (x ushr 17); x = x xor (x shl 5)
            sb.append(((x.toLong() and 0x7fffffffL) % 10L).toInt())
        }
        return sb.toString()
    }

    private fun bitmapFromView(v: View): Bitmap {
        val w = if (v.width > 0) v.width else resources.displayMetrics.widthPixels - (32 * resources.displayMetrics.density).toInt()
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        v.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            unspecified
        )
        v.layout(0, 0, v.measuredWidth, v.measuredHeight)

        val bmp = Bitmap.createBitmap(v.measuredWidth, v.measuredHeight, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        v.draw(c)
        return bmp
    }

    private fun shareImage() {
        try {
            val bmp = bitmapFromView(card)
            val dir = File(cacheDir, "receipts")
            if (!dir.exists()) dir.mkdirs()
            val f = File(dir, "receipt_$rawIndex.png")
            FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(i, "Share receipt"))
        } catch (ex: Exception) {
            Toast.makeText(this, "Share failed: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdf() {
        try {
            val bmp = bitmapFromView(card)
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, 1).create()
            val page = doc.startPage(pageInfo)
            page.canvas.drawBitmap(bmp, 0f, 0f, null)
            doc.finishPage(page)

            val dir = File(cacheDir, "receipts")
            if (!dir.exists()) dir.mkdirs()
            val f = File(dir, "receipt_$rawIndex.pdf")
            FileOutputStream(f).use { doc.writeTo(it) }
            doc.close()

            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(i, "Share receipt"))
        } catch (ex: Exception) {
            Toast.makeText(this, "PDF failed: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }
}