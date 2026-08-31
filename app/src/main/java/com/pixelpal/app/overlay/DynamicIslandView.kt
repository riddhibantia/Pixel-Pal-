package com.pixelpal.app.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A Dynamic Island-style reminder capsule pinned to the top-center of the screen.
 *
 * Bloom: appears as a full island (title + time + message + swipe hint) with a spring
 * scale/fade animation while the reminder rings, then compacts to a small capsule.
 * Tap toggles between the two sizes.
 *
 * Gestures: horizontal drags are forwarded to the owner (which moves the window);
 * on release the owner decides between committing the swipe and springing back.
 * The view itself never touches the WindowManager.
 */
class DynamicIslandView(
    context: Context,
    private val onDragDelta: (dx: Int) -> Unit,
    private val onRelease: (dx: Int, velocityX: Float) -> Unit
) : FrameLayout(context) {

    // Warm brown/gold palette — aligned with the app's DarkPalette
    private val islandBgTop = Color.parseColor("#2A1F16")    // Surface
    private val islandBgBottom = Color.parseColor("#1F1712")  // Background
    private val islandBorder = Color.parseColor("#F6C453")    // Primary gold
    private val textPrimary = Color.parseColor("#F5E9D2")    // Cream text
    private val textMuted = Color.parseColor("#CBB89A")      // Secondary text
    private val accentGreen = Color.parseColor("#F6C453")    // Primary gold (pulsing dot)
    private val accentYellow = Color.parseColor("#F6C453")   // Primary gold (time label)

    private val handler = Handler(Looper.getMainLooper())
    private var compactRunnable: Runnable? = null
    private var pulseAnimator: ValueAnimator? = null
    private var widthAnimator: ValueAnimator? = null

    private val density = resources.displayMetrics.density
    private val screenWidthPx = resources.displayMetrics.widthPixels
    private val compactWidthPx = (COMPACT_WIDTH_DP * density).toInt()
    private val expandedWidthPx = (screenWidthPx - (2 * ISLAND_MARGIN_DP * density)).toInt()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private val container: LinearLayout
    private val pulseDot: View
    private val titleView: TextView
    private val timeView: TextView
    private val messageView: TextView
    private val hintView: TextView

    private var isExpanded = false
    private var isGone = false

    // Gesture tracking
    private var downRawX = 0f
    private var downTime = 0L
    private var dragged = false
    private var velocityTracker: VelocityTracker? = null

    init {
        val paddingPx = (14 * density).toInt()

        val backgroundDrawable = GradientDrawable().apply {
            gradientType = GradientDrawable.LINEAR_GRADIENT
            colors = intArrayOf(islandBgTop, islandBgBottom)
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            cornerRadius = (26 * density)
            setStroke((1.5 * density).toInt(), islandBorder)
        }

        container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = backgroundDrawable
            elevation = 8 * density
            setPadding(paddingPx, (10 * density).toInt(), paddingPx, (10 * density).toInt())
            layoutParams = LayoutParams(expandedWidthPx, LayoutParams.WRAP_CONTENT)
        }

        // ---- Main row: pulsing dot + title + time ----
        val mainRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        pulseDot = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                (10 * density).toInt(),
                (10 * density).toInt()
            ).apply { marginEnd = (10 * density).toInt() }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accentGreen)
            }
        }
        mainRow.addView(pulseDot)

        titleView = TextView(context).apply {
            setTextColor(textPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        mainRow.addView(titleView)

        timeView = TextView(context).apply {
            setTextColor(accentYellow)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = (10 * density).toInt() }
        }
        mainRow.addView(timeView)

        container.addView(mainRow)

        messageView = TextView(context).apply {
            setTextColor(textPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setLineSpacing(0f, 1.2f)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, (6 * density).toInt(), 0, 0)
        }
        container.addView(messageView)

        hintView = TextView(context).apply {
            text = "Swipe → done   ·   ← snooze 10 min"
            setTextColor(textMuted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, (6 * density).toInt(), 0, 0)
        }
        container.addView(hintView)

        addView(container)

        isClickable = true

        // Bloom-in: tiny + transparent, springs open once populated
        container.alpha = 0f
        container.scaleX = 0.6f
        container.scaleY = 0.6f
    }

    fun showReminder(title: String, timeLabel: String, note: String?) {
        isGone = false
        titleView.text = title
        timeView.text = timeLabel
        messageView.text = note?.takeIf { it.isNotBlank() } ?: "It's time!"

        expand(immediateLayout = true)
        bloomIn()
        startPulse()
        scheduleAutoCompact()
    }

    private fun bloomIn() {
        container.pivotX = container.width / 2f
        container.pivotY = container.height / 2f
        container.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(320)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()
    }

    private fun startPulse() {
        stopPulse()
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 550
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                val t = it.animatedValue as Float
                pulseDot.alpha = 0.35f + 0.65f * t
                pulseDot.scaleX = 1f + 0.35f * t
                pulseDot.scaleY = 1f + 0.35f * t
            }
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
    }

    private fun scheduleAutoCompact() {
        cancelAutoCompact()
        val runnable = Runnable { if (isExpanded && !dragged) compact() }
        compactRunnable = runnable
        handler.postDelayed(runnable, AUTO_COMPACT_MS)
    }

    private fun cancelAutoCompact() {
        compactRunnable?.let { handler.removeCallbacks(it) }
        compactRunnable = null
    }

    private fun expand(immediateLayout: Boolean = false) {
        isExpanded = true
        messageView.visibility = VISIBLE
        hintView.visibility = VISIBLE
        animateWidthTo(expandedWidthPx, immediateLayout)
    }

    private fun compact() {
        isExpanded = false
        messageView.visibility = GONE
        hintView.visibility = GONE
        animateWidthTo(compactWidthPx)
    }

    private fun toggle() {
        if (isExpanded) compact() else expand()
    }

    private fun animateWidthTo(targetPx: Int, immediate: Boolean = false) {
        widthAnimator?.cancel()
        val start = if (container.layoutParams.width > 0) container.layoutParams.width else targetPx
        if (immediate || start == targetPx) {
            container.layoutParams.width = targetPx
            container.requestLayout()
            return
        }
        widthAnimator = ValueAnimator.ofInt(start, targetPx).apply {
            duration = 240
            addUpdateListener {
                if (isGone) return@addUpdateListener
                container.layoutParams.width = it.animatedValue as Int
                container.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    container.layoutParams.width = targetPx
                }
            })
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downTime = event.eventTime
                dragged = false
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(event)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                val dx = event.rawX - downRawX
                if (!dragged && kotlin.math.abs(dx) > touchSlop) {
                    dragged = true
                    cancelAutoCompact()
                }
                if (dragged) {
                    onDragDelta(dx.toInt())
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)
                val dx = event.rawX - downRawX
                if (dragged) {
                    onRelease(dx.toInt(), velocityTracker?.xVelocity ?: 0f)
                } else if (event.eventTime - downTime < TAP_TIMEOUT_MS) {
                    toggle()
                }
                velocityTracker?.recycle()
                velocityTracker = null
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.recycle()
                velocityTracker = null
                if (dragged) onRelease(0, 0f) // spring back
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun destroy() {
        isGone = true
        cancelAutoCompact()
        stopPulse()
        widthAnimator?.cancel()
        widthAnimator = null
        container.animate().cancel()
        handler.removeCallbacksAndMessages(null)
        velocityTracker?.recycle()
        velocityTracker = null
    }

    companion object {
        private const val AUTO_COMPACT_MS = 8_000L
        private const val TAP_TIMEOUT_MS = 250L
        private const val COMPACT_WIDTH_DP = 200f
        private const val ISLAND_MARGIN_DP = 16f
    }
}
