package com.pixelpal.app.overlay

import android.content.Context
import android.widget.FrameLayout
import android.widget.ImageView
import coil.load
import com.pixelpal.app.util.Constants

class CompanionOverlayView(context: Context) : FrameLayout(context) {

    val imageView: ImageView = ImageView(context)

    init {
        val sizePx = (Constants.OVERLAY_SIZE_DP * resources.displayMetrics.density).toInt()
        val params = LayoutParams(sizePx, sizePx)
        imageView.layoutParams = params
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        addView(imageView)
    }

    fun updateSprite(drawableRes: Int) {
        if (drawableRes != 0) {
            imageView.load(drawableRes) {
                crossfade(true)
            }
        }
    }
}
