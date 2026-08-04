package com.pixelpal.app.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.pixelpal.app.util.KeyboardStateManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class KeyboardAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var keyboardStateManager: KeyboardStateManager

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            checkKeyboardVisibility()
        }
    }
    
    private fun checkKeyboardVisibility() {
        var keyboardHeight = 0
        try {
            val windows = windows
            for (window in windows) {
                if (window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    val rect = Rect()
                    window.getBoundsInScreen(rect)
                    val screenHeight = resources.displayMetrics.heightPixels
                    val h = rect.bottom - rect.top
                    // Assume keyboard is at least 15% of screen height
                    if (h > screenHeight * 0.15f) {
                        keyboardHeight = h
                    }
                    break
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        keyboardStateManager.updateKeyboardHeight(keyboardHeight)
    }

    override fun onInterrupt() {
        keyboardStateManager.updateKeyboardHeight(0)
    }
}
