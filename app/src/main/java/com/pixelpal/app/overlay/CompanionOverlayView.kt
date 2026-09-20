package com.pixelpal.app.overlay

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.model.KeyPath
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.domain.model.PetTint
import com.pixelpal.app.util.Constants

/** Lottie-only overlay pet view (`res/raw/pet_{type}_{state}.json`). */
class CompanionOverlayView(context: Context) : FrameLayout(context) {

    private val lottieView: LottieAnimationView = LottieAnimationView(context)

    private var bodyArgb: Int? = null
    private var accentArgb: Int? = null

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
        // A fresh composition drops old value callbacks — re-attach.
        // Safe while parsing: Lottie queues pre-composition callbacks.
        reapplyTint()
    }

    /**
     * Recolors body+ears (color) and blush/sparkles (pattern). Stored and
     * re-applied on every state change; null keeps the baked art.
     */
    fun applyTint(bodyColorArgb: Int?, accentColorArgb: Int?) {
        bodyArgb = bodyColorArgb
        accentArgb = accentColorArgb
        val drawable = lottieView.drawable as? LottieDrawable ?: return
        bodyColorArgb?.let { c ->
            val callback = com.airbnb.lottie.value.LottieValueCallback<android.graphics.ColorFilter>(
                SimpleColorFilter(c)
            )
            PetTint.BODY_LAYERS.forEach { layer ->
                drawable.addValueCallback(KeyPath(layer, "**"), LottieProperty.COLOR_FILTER, callback)
            }
        }
        accentColorArgb?.let { c ->
            val callback = com.airbnb.lottie.value.LottieValueCallback<android.graphics.ColorFilter>(
                SimpleColorFilter(c)
            )
            PetTint.ACCENT_LAYERS.forEach { layer ->
                drawable.addValueCallback(KeyPath(layer, "**"), LottieProperty.COLOR_FILTER, callback)
            }
        }
        lottieView.invalidate()
    }

    private fun reapplyTint() {
        val body = bodyArgb
        val accent = accentArgb
        if (body == null && accent == null) return
        // Re-run through applyTint so callbacks attach to the new drawable.
        val storedBody = body
        val storedAccent = accent
        bodyArgb = null
        accentArgb = null
        applyTint(storedBody, storedAccent)
    }
}

