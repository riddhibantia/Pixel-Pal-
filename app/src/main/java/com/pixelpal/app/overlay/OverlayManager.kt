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
import android.animation.ValueAnimator
import android.view.animation.OvershootInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private var layoutParams: WindowManager.LayoutParams? = null
    private var bubbleLayoutParams: WindowManager.LayoutParams? = null

    private var currentX: Int = 0
    private var currentY: Int = 0
    private var autoDismissJob: Job? = null
    private var keyboardElevationJob: Job? = null
    private var savedX: Int = 0
    private var savedY: Int = 0
    private var isElevatedForKeyboard = false


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
                companionView?.let {
                    if (height > 0 && !isElevatedForKeyboard) {
                        savedX = currentX
                        savedY = currentY
                        isElevatedForKeyboard = true
                        val density = context.resources.displayMetrics.density
                        val screenHeight = context.resources.displayMetrics.heightPixels
                        val petSize = (Constants.OVERLAY_SIZE_DP * density).toInt()
                        val newY = screenHeight - height - petSize - (12 * density).toInt()
                        updatePosition(currentX, newY.coerceAtLeast(0))
                    } else if (height == 0 && isElevatedForKeyboard) {
                        isElevatedForKeyboard = false
                        updatePosition(savedX, savedY)
                    }
                }
            }
        }
    }



    fun hideCompanion() {
        keyboardElevationJob?.cancel()
        keyboardElevationJob = null
        hideSpeechBubble()
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

    fun showReminderPill(
        title: String,
        note: String?,
        onAccept: () -> Unit,
        onDeny: () -> Unit
    ) {
        autoDismissJob?.cancel()
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
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (Constants.OVERLAY_PILL_TOP_DP * density).toInt()
        }
        bubbleLayoutParams = params

        view.showReminderPill(
            title = title,
            note = note,
            onAccept = {
                hideSpeechBubble()
                onAccept()
            },
            onDeny = {
                hideSpeechBubble()
                onDeny()
            }
        )

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            speechBubbleView = null
        }
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
