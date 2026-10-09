package com.opay.clone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View

class ReceiptWatermarkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val TAG = "ReceiptWatermarkView"

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = 28
        isFilterBitmap = true
    }

    private var tile: Bitmap? = null
    private val m = Matrix()

    init {
        tile = try {
            BitmapFactory.decodeResource(resources, R.drawable.opay_water_mark)
        } catch (e: Exception) {
            Log.e(TAG, "decode failed: ${e.message}")
            null
        }
        Log.d(TAG, "tile = ${tile?.width} x ${tile?.height}")
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = tile
        if (bmp == null) {
            Log.w(TAG, "no bitmap to draw")
            return
        }
        if (width == 0 || height == 0) return

        val targetTileW = (width / 3f).coerceAtLeast(90f)
        val scale = targetTileW / bmp.width.toFloat()
        val tileW = bmp.width * scale
        val tileH = bmp.height * scale

        m.reset()
        m.postScale(scale, scale)
        m.postRotate(-30f)

        val overshoot = (tileW + tileH).toInt()
        var y = -overshoot.toFloat()
        while (y < height + overshoot) {
            var x = -overshoot.toFloat()
            while (x < width + overshoot) {
                val save = canvas.save()
                canvas.translate(x, y)
                canvas.concat(m)
                canvas.drawBitmap(bmp, 0f, 0f, paint)
                canvas.restore()
                x += tileW * 1.6f
            }
            y += tileH * 1.6f
        }
    }
}