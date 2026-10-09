package com.opay.clone

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

/**
 * A lightweight port of OPay's ImageLottieView:
 *   - shows a static image by default
 *   - if lottie_name + lottie_auto_play are set, plays a Lottie animation
 *   - else falls back to the static src
 */
class ImageLottieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    private val imageView: ImageView = ImageView(context)
    private val lottieView: LottieAnimationView = LottieAnimationView(context)

    init {
        addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(lottieView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        lottieView.visibility = View.GONE

        // Read attributes if provided
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.ImageLottieView)
            val lottieName = a.getString(R.styleable.ImageLottieView_lottie_name)
            val autoPlay = a.getBoolean(R.styleable.ImageLottieView_lottie_autoPlay, false)
            val loop = a.getBoolean(R.styleable.ImageLottieView_lottie_loop, false)
            val srcId = a.getResourceId(R.styleable.ImageLottieView_android_src, 0)
            val tint = a.getColor(R.styleable.ImageLottieView_android_tint, 0)
            a.recycle()

            if (srcId != 0) {
                imageView.setImageResource(srcId)
            }
            if (tint != 0) {
                imageView.setColorFilter(tint)
            }
            if (!lottieName.isNullOrEmpty()) {
                playLottie(lottieName, autoPlay, loop)
            }
        } else {
            // default = a small neutral icon
            imageView.setImageResource(android.R.drawable.ic_menu_view)
        }
    }

    fun setImageResource(resId: Int) {
        imageView.setImageResource(resId)
        lottieView.visibility = View.GONE
        imageView.visibility = View.VISIBLE
    }

    fun playLottie(name: String, autoPlay: Boolean = true, loop: Boolean = true) {
        try {
            lottieView.setAnimation(name)
            lottieView.repeatCount = if (loop) LottieDrawable.INFINITE else 0
            lottieView.visibility = View.VISIBLE
            imageView.visibility = View.GONE
            if (autoPlay) lottieView.playAnimation()
        } catch (_: Exception) {
            // fallback: keep static image
        }
    }
}
