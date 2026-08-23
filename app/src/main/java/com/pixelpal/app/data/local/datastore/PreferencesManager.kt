package com.pixelpal.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.pixelpal.app.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        val OVERLAY_COMPANION_IDS = stringSetPreferencesKey(Constants.KEY_OVERLAY_COMPANION_IDS)
        val PET_NAME = stringPreferencesKey(Constants.KEY_PET_NAME)
        val USER_NAME = stringPreferencesKey(Constants.KEY_USER_NAME)
        val USER_EMAIL = stringPreferencesKey(Constants.KEY_USER_EMAIL)
        val AVATAR_SEED = stringPreferencesKey(Constants.KEY_AVATAR_SEED)
        val SELECTED_PET_TYPE = stringPreferencesKey(Constants.KEY_SELECTED_PET_TYPE)
        val IS_FIRST_LAUNCH = booleanPreferencesKey(Constants.KEY_IS_FIRST_LAUNCH)
        val CURRENT_THEME = stringPreferencesKey(Constants.KEY_CURRENT_THEME)
        val ACTIVE_COMPANION_ID = longPreferencesKey(Constants.KEY_ACTIVE_COMPANION_ID)
        val COMPANION_BOOTSTRAP_DONE = booleanPreferencesKey(Constants.KEY_COMPANION_BOOTSTRAP_DONE)
        val SINGLE_COMPANION_FOLD_DONE = booleanPreferencesKey(Constants.KEY_SINGLE_COMPANION_FOLD_DONE)

        /** Per-companion overlay positions: overlay_x_<id> / overlay_y_<id>. */
        fun overlayX(companionId: Long) = floatPreferencesKey("${Constants.KEY_OVERLAY_X}_$companionId")
        fun overlayY(companionId: Long) = floatPreferencesKey("${Constants.KEY_OVERLAY_Y}_$companionId")
    }

    val overlayPosition: Flow<Pair<Float, Float>> = dataStore.data.map { preferences ->
        val x = preferences[Keys.OVERLAY_X] ?: Constants.OVERLAY_OFFSET_X_DP
        val y = preferences[Keys.OVERLAY_Y] ?: Constants.OVERLAY_OFFSET_Y_DP
        Pair(x, y)
    }

    val overlayEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.OVERLAY_ENABLED] ?: Constants.DEFAULT_OVERLAY_ENABLED
    }

    /** Companion ids (as strings) currently chosen for on-screen overlays. Empty = default to active companion. */
    val overlayCompanionIds: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[Keys.OVERLAY_COMPANION_IDS] ?: emptySet()
    }

    /** Per-companion persisted overlay position; falls back to the legacy global position. */
    fun overlayPositionFor(companionId: Long): Flow<Pair<Float, Float>> =
        dataStore.data.map { preferences ->
            val x = preferences[Keys.overlayX(companionId)]
                ?: preferences[Keys.OVERLAY_X]
                ?: Constants.OVERLAY_OFFSET_X_DP
            val y = preferences[Keys.overlayY(companionId)]
                ?: preferences[Keys.OVERLAY_Y]
                ?: Constants.OVERLAY_OFFSET_Y_DP
            Pair(x, y)
        }

    val petName: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.PET_NAME] ?: Constants.DEFAULT_PET_NAME
    }

    val userName: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.USER_NAME] ?: Constants.DEFAULT_USER_NAME
    }

    val userEmail: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.USER_EMAIL] ?: Constants.DEFAULT_USER_EMAIL
    }

    val avatarSeed: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.AVATAR_SEED] ?: Constants.DEFAULT_AVATAR_SEED
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

    /** The id of the currently active companion, or null when none is selected. */
    val activeCompanionId: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[Keys.ACTIVE_COMPANION_ID]
    }

    val companionBootstrapDone: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.COMPANION_BOOTSTRAP_DONE] ?: false
    }

    suspend fun updateOverlayPosition(x: Float, y: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.OVERLAY_X] = x
            preferences[Keys.OVERLAY_Y] = y
        }
    }

    /** Persists a drag end position for ONE companion's overlay. */
    suspend fun updateOverlayPositionFor(companionId: Long, x: Float, y: Float) {
        dataStore.edit { preferences ->
            preferences[Keys.overlayX(companionId)] = x
            preferences[Keys.overlayY(companionId)] = y
        }
    }

    suspend fun setOverlayCompanionIds(ids: Set<Long>) {
        dataStore.edit { preferences ->
            preferences[Keys.OVERLAY_COMPANION_IDS] = ids.map { it.toString() }.toSet()
        }
    }

    suspend fun clearOverlayPositions() {
        dataStore.edit { preferences ->
            val toRemove = preferences.asMap().keys.filter { key ->
                key.name.startsWith("${Constants.KEY_OVERLAY_X}_") ||
                    key.name.startsWith("${Constants.KEY_OVERLAY_Y}_")
            }
            toRemove.forEach { preferences.remove(it) }
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

    suspend fun getUserName(): String = dataStore.data.first()[Keys.USER_NAME] ?: Constants.DEFAULT_USER_NAME

    suspend fun getUserEmail(): String = dataStore.data.first()[Keys.USER_EMAIL] ?: Constants.DEFAULT_USER_EMAIL

    suspend fun getAvatarSeed(): String = dataStore.data.first()[Keys.AVATAR_SEED] ?: Constants.DEFAULT_AVATAR_SEED

    suspend fun getPetName(): String = dataStore.data.first()[Keys.PET_NAME] ?: Constants.DEFAULT_PET_NAME

    suspend fun getSelectedPetType(): String = dataStore.data.first()[Keys.SELECTED_PET_TYPE] ?: Constants.DEFAULT_SELECTED_PET_TYPE

    suspend fun getActiveCompanionId(): Long? = dataStore.data.first()[Keys.ACTIVE_COMPANION_ID]

    suspend fun isCompanionBootstrapDone(): Boolean =
        dataStore.data.first()[Keys.COMPANION_BOOTSTRAP_DONE] ?: false

    suspend fun isSingleCompanionFoldDone(): Boolean =
        dataStore.data.first()[Keys.SINGLE_COMPANION_FOLD_DONE] ?: false

    suspend fun setSingleCompanionFoldDone(done: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SINGLE_COMPANION_FOLD_DONE] = done
        }
    }

    suspend fun setUserEmail(email: String) {
        dataStore.edit { preferences ->
            preferences[Keys.USER_EMAIL] = email
        }
    }

    suspend fun setAvatarSeed(seed: String) {
        dataStore.edit { preferences ->
            preferences[Keys.AVATAR_SEED] = seed
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

    suspend fun setActiveCompanionId(companionId: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.ACTIVE_COMPANION_ID] = companionId
        }
    }

    suspend fun setCompanionBootstrapDone(done: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.COMPANION_BOOTSTRAP_DONE] = done
        }
    }
}
