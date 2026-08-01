package com.entropy.stage.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.entropy.stage.shell.StagePreferencesState
import com.entropy.stage.shell.StageSurface
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.stageDataStore by preferencesDataStore(name = "stage_desktop")

class StagePreferencesRepository(
    context: Context,
) {
    private val dataStore = context.stageDataStore

    val state: Flow<StagePreferencesState> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) {
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map(::toStagePreferences)

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.OnboardingComplete] = complete
        }
    }

    suspend fun setLastSurface(surface: StageSurface) {
        dataStore.edit { preferences ->
            preferences[Keys.LastSurface] = surface.name
        }
    }

    suspend fun togglePinned(appId: String) {
        dataStore.edit { preferences ->
            val current = decodePinned(preferences[Keys.PinnedApps]).toMutableSet()
            if (!current.add(appId)) current.remove(appId)
            preferences[Keys.PinnedApps] = current.joinToString(PIN_SEPARATOR)
        }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        dataStore.edit { it[Keys.ReduceMotion] = enabled }
    }

    suspend fun setDesktopGrain(enabled: Boolean) {
        dataStore.edit { it[Keys.DesktopGrain] = enabled }
    }

    suspend fun setDockMagnification(enabled: Boolean) {
        dataStore.edit { it[Keys.DockMagnification] = enabled }
    }

    private fun toStagePreferences(preferences: Preferences): StagePreferencesState =
        StagePreferencesState(
            onboardingComplete = preferences[Keys.OnboardingComplete] ?: false,
            pinnedAppIds = decodePinned(preferences[Keys.PinnedApps]),
            lastSurface = preferences[Keys.LastSurface]
                ?.let { saved -> runCatching { StageSurface.valueOf(saved) }.getOrNull() }
                ?: StageSurface.NONE,
            reduceMotion = preferences[Keys.ReduceMotion] ?: false,
            desktopGrain = preferences[Keys.DesktopGrain] ?: true,
            dockMagnification = preferences[Keys.DockMagnification] ?: true,
        )

    private object Keys {
        val OnboardingComplete = booleanPreferencesKey("onboarding_complete")
        val PinnedApps = stringPreferencesKey("pinned_app_ids")
        val LastSurface = stringPreferencesKey("last_surface")
        val ReduceMotion = booleanPreferencesKey("reduce_motion")
        val DesktopGrain = booleanPreferencesKey("desktop_grain")
        val DockMagnification = booleanPreferencesKey("dock_magnification")
    }

    private fun decodePinned(encoded: String?): Set<String> =
        encoded.orEmpty()
            .split(PIN_SEPARATOR)
            .filter(String::isNotBlank)
            .toCollection(LinkedHashSet())

    private companion object {
        const val PIN_SEPARATOR = "\n"
    }
}
