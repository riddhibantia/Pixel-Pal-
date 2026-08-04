package com.pixelpal.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyboardStateManager @Inject constructor() {
    private val _keyboardHeight = MutableStateFlow(0)
    val keyboardHeight: StateFlow<Int> = _keyboardHeight.asStateFlow()

    fun updateKeyboardHeight(height: Int) {
        _keyboardHeight.value = height
    }
}
