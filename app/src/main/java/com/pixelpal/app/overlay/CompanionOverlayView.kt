package com.pixelpal.app.overlay

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.util.Constants

/** Lottie-only overlay pet view (`res/raw/pet_{type}_{state}.json`). */
class CompanionOverlayView(context: Context) : FrameLayout(context) {

    private val lottieView: LottieAnimationView = LottieAnimationView(context)

    init {
        val sizePx = (Constants.OVERLAY_SIZE_DP * resources.displayMetrics.density).toInt()
        val params = LayoutParams(sizePx, sizePx)
        lottieView.layoutParams = params
        addView(lottieView)
    }

    /**
     * Lottie-only state player: looping states (sad/sleep/thinking/…)
     * run forever, one-shots play once.
     */
    fun playState(state: AnimationState, lottieRes: Int) {
        if (lottieRes == 0) {
            lottieView.cancelAnimation()
            lottieView.visibility = View.GONE
            return
        }
        lottieView.visibility = View.VISIBLE
        if (lottieView.tag != lottieRes) {
            lottieView.tag = lottieRes
            lottieView.setAnimation(lottieRes)
        }
        lottieView.repeatCount =
            if (state.loops) LottieDrawable.INFINITE else 0
        lottieView.playAnimation()
    }
}

