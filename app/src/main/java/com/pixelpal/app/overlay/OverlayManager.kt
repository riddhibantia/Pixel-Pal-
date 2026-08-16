package com.pixelpal.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.util.KeyboardStateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.OvershootInterpolator
import com.pixelpal.app.util.Constants

@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val keyboardStateManager: KeyboardStateManager
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var companionView: CompanionOverlayView? = null
    private var speechBubbleView: SpeechBubbleOverlayView? = null
    private var islandView: DynamicIslandView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var bubbleLayoutParams: WindowManager.LayoutParams? = null
    private var islandLayoutParams: WindowManager.LayoutParams? = null
    private var islandXAnimator: ValueAnimator? = null

    private var currentX: Int = 0
    private var currentY: Int = 0
    private var homeX: Int = 0
    private var homeY: Int = 0
    private var autoDismissJob: Job? = null
    private var keyboardElevationJob: Job? = null
    private var isElevatedForKeyboard = false
    private var currentKeyboardHeight = 0


    fun showCompanion(
        onTap: () -> Unit,
        onDoubleTap: (() -> Unit)? = null,
        onLongPress: (() -> Unit)? = null
    ) {
        if (companionView != null) return

        val view = CompanionOverlayView(context)
        companionView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        layoutParams = params

        scope.launch {
            val (x, y) = preferencesManager.overlayPosition.first()
            currentX = x.toInt()
            currentY = y.toInt()
            homeX = currentX
            homeY = currentY
            params.x = currentX
            params.y = currentY

            val touchHandler = OverlayTouchHandler(
                overlayManager = this@OverlayManager,
                onTap = onTap,
                onDoubleTap = onDoubleTap,
                onLongPress = onLongPress,
                onDragEnd = { finalX, finalY ->
                    scope.launch {
                        preferencesManager.updateOverlayPosition(finalX, finalY)
                    }
                    homeX = finalX.toInt()
                    homeY = finalY.toInt()
                    applyKeyboardDodge()
                }
            )
            view.setOnTouchListener(touchHandler)

            try {
                windowManager.addView(view, params)
            } catch (e: Exception) {
                companionView = null
                return@launch
            }

            observeKeyboardHeight()
        }
    }

    private fun observeKeyboardHeight() {
        keyboardElevationJob?.cancel()
        keyboardElevationJob = scope.launch {
            keyboardStateManager.keyboardHeight.collect { height ->
                currentKeyboardHeight = height
                applyKeyboardDodge()
            }
        }
    }

    /**
     * Lifts the pet above the keyboard ONLY if it is currently sitting in the
     * keyboard area. If the pet is already above the keyboard region it stays put.
     */
    private fun applyKeyboardDodge() {
        companionView ?: return

        val density = context.resources.displayMetrics.density
        val threshold = (Constants.KEYBOARD_DODGE_THRESHOLD_DP * density).toInt()
        val screenHeight = context.resources.displayMetrics.heightPixels
        val petSize = (Constants.OVERLAY_SIZE_DP * density).toInt()

        val keyboardOpen = currentKeyboardHeight > threshold
        if (keyboardOpen) {
            val keyboardTop = screenHeight - currentKeyboardHeight
            val petBottom = homeY + petSize
            val overlapsKeyboard = petBottom > keyboardTop

            if (overlapsKeyboard && !isElevatedForKeyboard) {
                isElevatedForKeyboard = true
                val newY = screenHeight - currentKeyboardHeight - petSize - (12 * density).toInt()
                updatePosition(homeX, newY.coerceAtLeast(0))
            } else if (!overlapsKeyboard && isElevatedForKeyboard) {
                isElevatedForKeyboard = false
                updatePosition(homeX, homeY)
            }
        } else if (isElevatedForKeyboard) {
            isElevatedForKeyboard = false
            updatePosition(homeX, homeY)
        }
    }



    fun hideCompanion() {
        keyboardElevationJob?.cancel()
        keyboardElevationJob = null
        hideSpeechBubble()
        hideDynamicIsland()
        companionView?.let { view ->
            try { windowManager.removeView(view) } catch (e: Exception) {}
            companionView = null
        }
    }

    fun updatePosition(x: Int, y: Int) {
        currentX = x
        currentY = y
        layoutParams?.let { params ->
            params.x = x
            params.y = y
            companionView?.let { view ->
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        updateBubblePosition()
    }

    fun showSpeechBubble(
        text: String,
        onDone: (() -> Unit)? = null,
        onSnooze: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null
    ) {
        autoDismissJob?.cancel()
        removeDynamicIsland()
        // Remove any existing bubble synchronously — do NOT use hideSpeechBubble() here
        // because animateOut is async and its callback would null the NEW speechBubbleView.
        speechBubbleView?.let { oldView ->
            try { windowManager.removeView(oldView) } catch (_: Exception) {}
        }
        speechBubbleView = null

        val view = SpeechBubbleOverlayView(context)
        speechBubbleView = view

        val density = context.resources.displayMetrics.density
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = currentX - (40 * density).toInt()
            y = (currentY - (80 * density).toInt()).coerceAtLeast(0)
        }
        bubbleLayoutParams = params

        view.showText(text)

        if (onDone != null && onSnooze != null && onDismiss != null) {
            view.showActions(
                onDone = {
                    hideSpeechBubble()
                    onDone()
                },
                onSnooze = {
                    hideSpeechBubble()
                    onSnooze()
                },
                onDismiss = {
                    hideSpeechBubble()
                    onDismiss()
                }
            )
        } else {
            view.setOnClickListener { hideSpeechBubble() }
            autoDismissJob = scope.launch {
                delay(6000L)
                hideSpeechBubble()
            }
        }

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            speechBubbleView = null
        }
    }

    /**
     * Shows the Dynamic Island reminder at the top-center of the screen. The island
     * owns its own gesture detection but forwards drags here — the window position
     * (params.x around the horizontal center) is managed exclusively by this class.
     */
    fun showDynamicIsland(
        title: String,
        timeLabel: String,
        note: String?,
        onComplete: () -> Unit,
        onSnooze: () -> Unit
    ) {
        autoDismissJob?.cancel()
        speechBubbleView?.let { oldView ->
            try { windowManager.removeView(oldView) } catch (_: Exception) {}
        }
        speechBubbleView = null
        removeDynamicIsland()

        val view = DynamicIslandView(
            context = context,
            onDragDelta = { dx -> moveIslandBy(dx) },
            onRelease = { dx, velocity -> resolveIslandSwipe(dx, velocity, onComplete, onSnooze) }
        )
        islandView = view

        val density = context.resources.displayMetrics.density
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            // FLAG_LAYOUT_IN_SCREEN + a tiny y offset makes the island hug the
            // physical top edge over the status bar / camera cutout, like the
            // iOS Dynamic Island.
            y = (Constants.OVERLAY_ISLAND_TOP_DP * density).toInt()
        }
        islandLayoutParams = params

        view.showReminder(
            title = title,
            timeLabel = timeLabel,
            note = note
        )

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            islandView = null
            islandLayoutParams = null
        }
    }

    private fun moveIslandBy(dx: Int) {
        val params = islandLayoutParams ?: return
        val view = islandView ?: return
        islandXAnimator?.cancel()
        params.x = dx
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun resolveIslandSwipe(dx: Int, velocityX: Float, onComplete: () -> Unit, onSnooze: () -> Unit) {
        val view = islandView ?: return
        val threshold = view.width.coerceAtLeast(1) * 0.35f
        val flingVelocity = 2000f * context.resources.displayMetrics.density
        when {
            dx > threshold || velocityX > flingVelocity ->
                animateIslandOffscreen(direction = 1, onDone = onComplete)
            dx < -threshold || velocityX < -flingVelocity ->
                animateIslandOffscreen(direction = -1, onDone = onSnooze)
            else -> animateIslandBackToCenter()
        }
    }

    private fun animateIslandOffscreen(direction: Int, onDone: () -> Unit) {
        val params = islandLayoutParams ?: return onDone()
        val target = direction * context.resources.displayMetrics.widthPixels
        animateIslandX(params.x, target) {
            removeDynamicIsland()
            onDone()
        }
    }

    private fun animateIslandBackToCenter() {
        val params = islandLayoutParams ?: return
        animateIslandX(params.x, 0, spring = true)
    }

    private fun animateIslandX(from: Int, to: Int, spring: Boolean = false, onEnd: () -> Unit = {}) {
        val params = islandLayoutParams ?: return onEnd()
        islandXAnimator?.cancel()
        islandXAnimator = ValueAnimator.ofInt(from, to).apply {
            duration = if (spring) 260 else 200
            if (spring) interpolator = OvershootInterpolator(1.2f)
            addUpdateListener {
                val view = islandView ?: return@addUpdateListener
                params.x = it.animatedValue as Int
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: Exception) {
                    // ignore
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })
            start()
        }
    }

    fun hideDynamicIsland() {
        removeDynamicIsland()
    }

    private fun removeDynamicIsland() {
        islandXAnimator?.cancel()
        islandXAnimator = null
        islandView?.let { view ->
            try { windowManager.removeView(view) } catch (e: Exception) {}
            view.destroy()
        }
        islandView = null
        islandLayoutParams = null
    }

    fun hideSpeechBubble() {
        autoDismissJob?.cancel()
        autoDismissJob = null
        speechBubbleView?.let { view ->
            view.animateOut {
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    // ignore
                }
                speechBubbleView = null
            }
        }
    }

    private fun updateBubblePosition() {
        speechBubbleView?.let { view ->
            bubbleLayoutParams?.let { params ->
                val density = context.resources.displayMetrics.density
                params.x = currentX - (40 * density).toInt()
                params.y = (currentY - (80 * density).toInt()).coerceAtLeast(0)
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    fun updateSprite(drawableRes: Int) {
        companionView?.updateSprite(drawableRes)
    }

    fun getCurrentX(): Int = currentX
    fun getCurrentY(): Int = currentY
    fun isShowing(): Boolean = companionView != null
}
