package com.pixelpal.app.overlay

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class SpeechBubbleOverlayView(context: Context) : FrameLayout(context) {

    val textView = TextView(context)
    val actionRow = LinearLayout(context)
    private val container: LinearLayout
    private val headerRow = LinearLayout(context)

    private val handler = Handler(Looper.getMainLooper())
    private var typewriterRunnable: Runnable? = null

    // Warm brown/gold palette — aligned with the app's DarkPalette
    private val bubbleBg = Color.parseColor("#2A1F16")       // Surface
    private val bubbleBorder = Color.parseColor("#F6C453")   // Primary gold
    private val textPrimary = Color.parseColor("#F5E9D2")   // Cream text
    private val textMuted = Color.parseColor("#CBB89A")     // Secondary text
    private val accentGreen = Color.parseColor("#6BCB77")   // PixelSuccess
    private val accentYellow = Color.parseColor("#FFD166")  // PixelWarning
    private val accentRed = Color.parseColor("#FF6B6B")     // Error
    private val accentPurple = Color.parseColor("#8B6CF6")  // Secondary purple

    init {
        val density = resources.displayMetrics.density
        val paddingPx = (14 * density).toInt()

        // Outer glow effect
        val outerGlow = GradientDrawable().apply {
            setColor(Color.parseColor("#F6C453"))  // Primary gold
            cornerRadius = 20 * density
            setStroke((1 * density).toInt(), Color.parseColor("#1AF6C453"))  // Primary gold 10% alpha
        }

        // Main bubble background with gradient feel
        val backgroundDrawable = GradientDrawable().apply {
            gradientType = GradientDrawable.LINEAR_GRADIENT
            colors = intArrayOf(
                Color.parseColor("#33261C"),  // SurfaceElevated
                Color.parseColor("#2A1F16")   // Surface
            )
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            setStroke((1.5 * density).toInt(), bubbleBorder)
            cornerRadius = 18 * density
        }

        container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            background = backgroundDrawable
            elevation = 6 * density
        }

        // Header row with icon and "says" label
        headerRow.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, (8 * density).toInt())
        }

        val dotIndicator = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                (6 * density).toInt(),
                (6 * density).toInt()
            ).apply {
                marginEnd = (6 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accentGreen)
            }
        }
        headerRow.addView(dotIndicator)

        val headerLabel = TextView(context).apply {
            text = "says"
            setTextColor(textMuted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        headerRow.addView(headerLabel)
        container.addView(headerRow)

        // Message text
        textView.apply {
            setTextColor(textPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setLineSpacing(0f, 1.15f)
            letterSpacing = 0.01f
        }
        container.addView(textView)

        // Action row
        actionRow.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            visibility = View.GONE
            setPadding(0, (10 * density).toInt(), 0, 0)
        }
        container.addView(actionRow)

        val params = LayoutParams(
            (230 * density).toInt(),
            LayoutParams.WRAP_CONTENT
        )
        container.layoutParams = params
        addView(container)

        // Animate in
        container.alpha = 0f
        container.scaleX = 0.85f
        container.scaleY = 0.85f
    }

    fun animateIn() {
        container.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(250)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()
    }

    fun animateOut(onComplete: (() -> Unit)? = null) {
        container.animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(180)
            .withEndAction { onComplete?.invoke() }
            .start()
    }

    fun showText(text: String, onComplete: (() -> Unit)? = null) {
        typewriterRunnable?.let { handler.removeCallbacks(it) }
        textView.text = ""
        animateIn()

        var index = 0
        typewriterRunnable = object : Runnable {
            override fun run() {
                if (index <= text.length) {
                    textView.text = text.substring(0, index++)
                    handler.postDelayed(this, 25L)
                } else {
                    onComplete?.invoke()
                }
            }
        }
        handler.post(typewriterRunnable!!)
    }

    fun showActions(
        onDone: () -> Unit,
        onSnooze: () -> Unit,
        onDismiss: () -> Unit
    ) {
        actionRow.removeAllViews()
        actionRow.visibility = View.VISIBLE

        val doneBtn = createStyledButton("Done", accentGreen) { onDone() }
        val snoozeBtn = createStyledButton("Snooze", accentYellow) { onSnooze() }
        val dismissBtn = createStyledButton("Dismiss", accentRed) { onDismiss() }

        actionRow.addView(dismissBtn)
        actionRow.addView(snoozeBtn)
        actionRow.addView(doneBtn)
    }

    fun hideActions() {
        actionRow.visibility = View.GONE
    }

    private fun createStyledButton(text: String, color: Int, onClick: () -> Unit): Button {
        val density = resources.displayMetrics.density
        return Button(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(color)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setBackgroundColor(Color.TRANSPARENT)

            val bg = GradientDrawable().apply {
                setColor(color and 0x00FFFFFF or 0x1A000000) // 10% alpha
                cornerRadius = 8 * density
                setStroke((1 * density).toInt(), color and 0x33FFFFFF)
            }
            background = bg

            setPadding(
                (10 * density).toInt(),
                (4 * density).toInt(),
                (10 * density).toInt(),
                (4 * density).toInt()
            )
            minimumHeight = 0
            minimumWidth = 0
            isAllCaps = false

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (28 * density).toInt()
            ).apply {
                marginStart = (6 * density).toInt()
            }
            layoutParams = params

            setOnClickListener { onClick() }
        }
    }
}
