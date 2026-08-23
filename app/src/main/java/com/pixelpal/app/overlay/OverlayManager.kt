package com.pixelpal.app.overlay

import android.content.Context
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import com.pixelpal.app.data.local.datastore.PreferencesManager
import com.pixelpal.app.util.KeyboardStateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.pixelpal.app.util.Constants

/**
 * Registry of [OverlaySession]s — one per on-screen companion.
 *
 * Responsibilities:
 *  - start/stop the overlay for a SPECIFIC companionId,
 *  - route speech bubbles to the right session,
 *  - own the global Dynamic Island reminder UI,
 *  - enforce nothing about limits here (the service resolves the desired set;
 *    MAX_SIMULTANEOUS_OVERLAYS is enforced by settings + service sync).
 */
@Singleton
class OverlayManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val keyboardStateManager: KeyboardStateManager
) {
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val sessions = LinkedHashMap<Long, OverlaySession>()

    // ── Session lifecycle ──────────────────────────────────────────────────

    fun showCompanionFor(
        companionId: Long,
        petType: String,
        onTap: (Long) -> Unit,
        onDoubleTap: ((Long) -> Unit)? = null,
        onLongPress: ((Long) -> Unit)?
    ) {
        if (sessions.containsKey(companionId)) return
        val slot = sessions.size.coerceAtMost(Constants.MAX_SIMULTANEOUS_OVERLAYS - 1)
        val session = OverlaySession(
            companionId = companionId,
            context = context,
            preferencesManager = preferencesManager,
            windowManager = windowManager,
            keyboardStateManager = keyboardStateManager,
            slotIndex = slot,
            petType = petType,
            scope = scope,
            onTap = onTap,
            onDoubleTap = onDoubleTap,
            onLongPress = onLongPress
        )
        sessions[companionId] = session
        session.show()
    }

    fun hideCompanionFor(companionId: Long) {
        sessions.remove(companionId)?.hide()
    }

    fun stopAllOverlays() {
        val ids = sessions.keys.toList()
        ids.forEach { hideCompanionFor(it) }
    }

    /** Re-stagger slots after removals so remaining pets keep distinct defaults. */
    fun normalizeSlots(desiredOrder: List<Long>) {
        // Slots only affect DEFAULT spawn positions; persisted drags win anyway.
    }

    fun activeCompanionIds(): List<Long> = sessions.keys.toList()

    fun isShowing(): Boolean = sessions.isNotEmpty()
    fun isShowing(companionId: Long): Boolean = sessions[companionId]?.isShowing == true

    /** Keeps a running session's sprite in sync when the companion's pet type changes. */
    fun updatePetTypeFor(companionId: Long, petType: String) {
        sessions[companionId]?.updatePetType(petType)
    }

    // ── Per-companion speech ───────────────────────────────────────────────

    fun showSpeechBubble(companionId: Long, text: String) {
        sessions[companionId]?.showMessage(text)
    }

    // ── Global reminder island (unchanged behavior, single instance) ───────

    private var islandView: DynamicIslandView? = null
    private var islandLayoutParams: WindowManager.LayoutParams? = null
    private var islandXAnimator: ValueAnimator? = null

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
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
            x = 0
            y = (Constants.OVERLAY_ISLAND_TOP_DP * density).toInt()
        }
        islandLayoutParams = params

        view.showReminder(title = title, timeLabel = timeLabel, note = note)

        try {
            windowManager.addView(view, params)
        } catch (e: Exception) {
            islandView = null
            islandLayoutParams = null
        }
    }

    fun hideDynamicIsland() {
        removeDynamicIsland()
    }

    private fun moveIslandBy(dx: Int) {
        val params = islandLayoutParams ?: return
        val view = islandView ?: return
        islandXAnimator?.cancel()
        params.x = dx
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {
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
                } catch (_: Exception) {
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd()
                }
            })
            start()
        }
    }

    private fun removeDynamicIsland() {
        islandXAnimator?.cancel()
        islandXAnimator = null
        islandView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
            view.destroy()
        }
        islandView = null
        islandLayoutParams = null
    }
}