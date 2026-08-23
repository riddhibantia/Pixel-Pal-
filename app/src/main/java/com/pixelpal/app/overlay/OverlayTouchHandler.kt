package com.pixelpal.app.overlay

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class OverlayTouchHandler(
    private val overlayManager: Manager,
    private val onTap: () -> Unit,
    private val onDoubleTap: (() -> Unit)? = null,
    private val onLongPress: (() -> Unit)? = null,
    private val onDragEnd: ((x: Float, y: Float) -> Unit)? = null
) : View.OnTouchListener {

    /** Minimal position provider — implemented by OverlayManager and by each OverlaySession. */
    interface Manager {
        fun getCurrentX(): Int
        fun getCurrentY(): Int
        fun updatePosition(x: Int, y: Int)
    }

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var isDragging = false

    private val tapThreshold = 10
    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var lastTapTime = 0L

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = overlayManager.getCurrentX()
                initialY = overlayManager.getCurrentY()
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false

                longPressRunnable = Runnable {
                    if (!isDragging) {
                        onLongPress?.invoke()
                    }
                }
                handler.postDelayed(longPressRunnable!!, 500L)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.rawX - initialTouchX)
                val dy = abs(event.rawY - initialTouchY)

                if (dx > tapThreshold || dy > tapThreshold) {
                    isDragging = true
                    longPressRunnable?.let { handler.removeCallbacks(it) }

                    val newX = initialX + (event.rawX - initialTouchX).toInt()
                    val newY = initialY + (event.rawY - initialTouchY).toInt()
                    overlayManager.updatePosition(newX, newY)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                longPressRunnable?.let { handler.removeCallbacks(it) }

                if (!isDragging) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTapTime < 300L) {
                        onDoubleTap?.invoke()
                    } else {
                        onTap()
                    }
                    lastTapTime = currentTime
                } else {
                    onDragEnd?.invoke(
                        overlayManager.getCurrentX().toFloat(),
                        overlayManager.getCurrentY().toFloat()
                    )
                }
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                longPressRunnable?.let { handler.removeCallbacks(it) }
                return true
            }
        }
        return false
    }
}