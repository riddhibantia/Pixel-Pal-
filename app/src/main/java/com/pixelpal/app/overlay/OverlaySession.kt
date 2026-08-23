package com.pixelpal.app.overlay

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import com.pixelpal.app.animation.AnimationState
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.util.Constants
import com.pixelpal.app.util.KeyboardStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One overlay instance bound to exactly ONE companion. Owns its view,
 * position, speech bubble and keyboard-dodge behavior — sessions never share
 * identity or state with each other.
 */
class OverlaySession(
    val companionId: Long,
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val windowManager: WindowManager,
    private val keyboardStateManager: KeyboardStateManager,
    val slotIndex: Int,
    petType: String,
    private val scope: CoroutineScope,
    private val onTap: (Long) -> Unit,
    private val onDoubleTap: ((Long) -> Unit)?,
    private val onLongPress: ((Long) -> Unit)?
) {
    val renderer = SessionSpriteRenderer(context, petType)

    var view: CompanionOverlayView? = null
        private set
    private var params: WindowManager.LayoutParams? = null
    private var bubbleView: SpeechBubbleOverlayView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var bubbleDismissJob: Job? = null
    private var keyboardJob: Job? = null
    private var reactionJob: Job? = null

    private var currentX = 0
    private var currentY = 0
    private var homeX = 0
    private var homeY = 0
    private var keyboardHeight = 0
    private var elevatedForKeyboard = false

    val isShowing: Boolean get() = view != null

    fun show(onReady: () -> Unit = {}) {
        if (view != null) return
        val overlayView = CompanionOverlayView(context)
        view = overlayView

        val density = context.resources.displayMetrics.density
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        params = layoutParams

        // Default positions stagger per slot so two pets never spawn stacked.
        val defaultX = Constants.OVERLAY_OFFSET_X_DP + slotIndex * SLOT_STAGGER_X_DP
        val defaultY = Constants.OVERLAY_OFFSET_Y_DP + slotIndex * SLOT_STAGGER_Y_DP

        scope.launch {
            val saved = preferencesManager.overlayPositionFor(companionId).firstOrNull()
            val usesDefault =
                saved == null ||
                    (saved.first == Constants.OVERLAY_OFFSET_X_DP && saved.second == Constants.OVERLAY_OFFSET_Y_DP)
            val x: Float
            val y: Float
            if (usesDefault) {
                x = defaultX
                y = defaultY
            } else {
                x = saved.first
                y = saved.second
            }
            currentX = x.toInt()
            currentY = y.toInt()
            homeX = currentX
            homeY = currentY
            layoutParams.x = currentX
            layoutParams.y = currentY

            overlayView.setOnTouchListener(
                OverlayTouchHandler(
                    overlayManager = TouchBridge(),
                    onTap = { onTap(companionId); flashReaction(AnimationState.HAPPY) },
                    onDoubleTap = { onDoubleTap?.invoke(companionId) },
                    onLongPress = { onLongPress?.invoke(companionId) },
                    onDragEnd = { finalX, finalY ->
                        scope.launch {
                            preferencesManager.updateOverlayPositionFor(companionId, finalX, finalY)
                        }
                        homeX = finalX.toInt()
                        homeY = finalY.toInt()
                        applyKeyboardDodge()
                    }
                )
            )

            try {
                windowManager.addView(overlayView, layoutParams)
            } catch (e: Exception) {
                view = null
                return@launch
            }

            renderer.drawableFor(AnimationState.IDLE).takeIf { it != 0 }?.let {
                overlayView.updateSprite(it)
            }
            observeKeyboard()
            onReady()
        }
    }

    fun hide() {
        keyboardJob?.cancel()
        keyboardJob = null
        reactionJob?.cancel()
        reactionJob = null
        hideBubble()
        view?.let { v ->
            try {
                windowManager.removeView(v)
            } catch (_: Exception) {
            }
        }
        view = null
        params = null
    }

    fun updatePetType(petType: String) {
        if (renderer.petType != petType) {
            renderer.updatePetType(petType)
            renderer.drawableFor(AnimationState.IDLE).takeIf { it != 0 }?.let {
                view?.updateSprite(it)
            }
        }
    }

    /** Shows THIS session's speech bubble anchored above ITS OWN pet. */
    fun showMessage(text: String) {
        bubbleDismissJob?.cancel()
        bubbleView?.let { old ->
            try {
                windowManager.removeView(old)
            } catch (_: Exception) {
            }
        }
        bubbleView = null

        val bubble = SpeechBubbleOverlayView(context)
        bubbleView = bubble

        val density = context.resources.displayMetrics.density
        val layoutParams = WindowManager.LayoutParams(
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
        bubbleParams = layoutParams

        bubble.showText(text)
        bubble.setOnClickListener { hideBubble() }

        try {
            windowManager.addView(bubble, layoutParams)
        } catch (_: Exception) {
            bubbleView = null
            return
        }

        bubbleDismissJob = scope.launch {
            delay(4000L)
            hideBubble()
        }
    }

    fun repositionBubble() {
        val bubble = bubbleView ?: return
        val layout = bubbleParams ?: return
        val density = context.resources.displayMetrics.density
        layout.x = currentX - (40 * density).toInt()
        layout.y = (currentY - (80 * density).toInt()).coerceAtLeast(0)
        try {
            windowManager.updateViewLayout(bubble, layout)
        } catch (_: Exception) {
        }
    }

    private fun hideBubble() {
        bubbleDismissJob?.cancel()
        bubbleDismissJob = null
        bubbleView?.let { b ->
            try {
                windowManager.removeView(b)
            } catch (_: Exception) {
            }
        }
        bubbleView = null
        bubbleParams = null
    }

    private fun flashReaction(state: AnimationState) {
        val idle = renderer.drawableFor(AnimationState.IDLE).takeIf { it != 0 } ?: return
        val target = renderer.drawableFor(state).takeIf { it != 0 } ?: return
        reactionJob?.cancel()
        reactionJob = scope.launch {
            view?.updateSprite(target)
            delay(700)
            view?.updateSprite(idle)
        }
    }

    private fun observeKeyboard() {
        keyboardJob?.cancel()
        keyboardJob = scope.launch {
            keyboardStateManager.keyboardHeight.collect { height ->
                keyboardHeight = height
                applyKeyboardDodge()
            }
        }
    }

    private fun applyKeyboardDodge() {
        if (view == null) return
        val density = context.resources.displayMetrics.density
        val threshold = (Constants.KEYBOARD_DODGE_THRESHOLD_DP * density).toInt()
        val screenHeight = context.resources.displayMetrics.heightPixels
        val petSize = (Constants.OVERLAY_SIZE_DP * density).toInt()

        val keyboardOpen = keyboardHeight > threshold
        if (keyboardOpen) {
            val keyboardTop = screenHeight - keyboardHeight
            val overlapsKeyboard = homeY + petSize > keyboardTop
            if (overlapsKeyboard && !elevatedForKeyboard) {
                elevatedForKeyboard = true
                val newY = screenHeight - keyboardHeight - petSize - (12 * density).toInt()
                updatePosition(homeX, newY.coerceAtLeast(0))
            } else if (!overlapsKeyboard && elevatedForKeyboard) {
                elevatedForKeyboard = false
                updatePosition(homeX, homeY)
            }
        } else if (elevatedForKeyboard) {
            elevatedForKeyboard = false
            updatePosition(homeX, homeY)
        }
    }

    fun updatePosition(x: Int, y: Int) {
        currentX = x
        currentY = y
        params?.let { p ->
            p.x = x
            p.y = y
            view?.let { v ->
                try {
                    windowManager.updateViewLayout(v, p)
                } catch (_: Exception) {
                }
            }
        }
        repositionBubble()
    }

    /**
     * The legacy [OverlayTouchHandler] expects an OverlayManager for position
     * reads/updates during drags; each session bridges those calls to itself.
     */
    private inner class TouchBridge : OverlayTouchHandler.Manager {
        override fun getCurrentX(): Int = currentX
        override fun getCurrentY(): Int = currentY
        override fun updatePosition(x: Int, y: Int) = this@OverlaySession.updatePosition(x, y)
    }

    companion object {
        /** Default spawn stagger so two pets never start stacked on each other. */
        private const val SLOT_STAGGER_X_DP = 90f
        private const val SLOT_STAGGER_Y_DP = 120f
    }
}