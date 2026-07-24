package com.pixelpal.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pixelpal.app.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val OVERLAY_X = floatPreferencesKey(Constants.KEY_OVERLAY_X)
        val OVERLAY_Y = floatPreferencesKey(Constants.KEY_OVERLAY_Y)
        val OVERLAY_ENABLED = booleanPreferencesKey(Constants.KEY_OVERLAY_ENABLED)
        val PET_NAME = stringPreferencesKey(Constants.KEY_PET_NAME)
        val USER_NAME = stringPreferencesKey(Constants.KEY_USER_NAME)
        val SELECTED_PET_TYPE = stringPreferencesKey(Constants.KEY_SELECTED_PET_TYPE)
        val IS_FIRST_LAUNCH = booleanPreferencesKey(Constants.KEY_IS_FIRST_LAUNCH)
        val CURRENT_THEME = stringPreferencesKey(Constants.KEY_CURRENT_THEME)
    }

    val overlayPosition: Flow<Pair<Float, Float>> = dataStore.data.map { preferences ->
        val x = preferences[Keys.OVERLAY_X] ?: Constants.OVERLAY_OFFSET_X_DP
        val y = preferences[Keys.OVERLAY_Y] ?: Constants.OVERLAY_OFFSET_Y_DP
        Pair(x, y)
    }

    val overlayEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.OVERLAY_ENABLED] ?: Constants.DEFAULT_OVERLAY_ENABLED
    }

    val petName: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.PET_NAME] ?: Constants.DEFAULT_PET_NAME
    }

    val userName: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.USER_NAME] ?: Constants.DEFAULT_USER_NAME
    }

    val selectedPetType: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.SELECTED_PET_TYPE] ?: Constants.DEFAULT_SELECTED_PET_TYPE
    }

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.IS_FIRST_LAUNCH] ?: Constants.DEFAULT_IS_FIRST_LAUNCH
    }

    val currentTheme: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.CURRENT_THEME] ?: Constants.DEFAULT_CURRENT_THEME
    }

    suspend fun updateOverlayPosition(x: Float, y: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.OVERLAY_X] = x
            preferences[Keys.OVERLAY_Y] = y
        }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun setPetName(name: String) {
        dataStore.edit { preferences ->
            preferences[Keys.PET_NAME] = name
        }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[Keys.USER_NAME] = name
        }
    }

    suspend fun setSelectedPetType(type: String) {
        dataStore.edit { preferences ->
            preferences[Keys.SELECTED_PET_TYPE] = type
        }
    }

    suspend fun setIsFirstLaunch(isFirst: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.IS_FIRST_LAUNCH] = isFirst
        }
    }

    suspend fun setCurrentTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[Keys.CURRENT_THEME] = theme
        }
    }
}
