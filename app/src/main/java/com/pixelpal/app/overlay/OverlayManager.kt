package com.pixelpal.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import com.pixelpal.app.data.local.datastore.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var companionView: CompanionOverlayView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var currentX: Int = 0
    private var currentY: Int = 0

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
            }
        }
    }

    fun hideCompanion() {
        companionView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // ignore
            }
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
    }

    fun updateSprite(drawableRes: Int) {
        companionView?.updateSprite(drawableRes)
    }

    fun getCurrentX(): Int = currentX
    fun getCurrentY(): Int = currentY
    fun isShowing(): Boolean = companionView != null
}
