package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mapa_prefs")

class PreferencesRepository(private val context: Context) {
  private val selectedPatioKey = stringPreferencesKey("selected_patio_id")
  private val cachedDisplayNameKey = stringPreferencesKey("cached_display_name")
  private val cachedWorkspaceIdKey = stringPreferencesKey("cached_workspace_id")

  val selectedPatioId: Flow<String?> =
    context.dataStore.data.map { prefs -> prefs[selectedPatioKey] }

  val cachedDisplayName: Flow<String?> =
    context.dataStore.data.map { prefs -> prefs[cachedDisplayNameKey] }

  val cachedWorkspaceId: Flow<String?> =
    context.dataStore.data.map { prefs -> prefs[cachedWorkspaceIdKey] }

  suspend fun saveSelectedPatioId(patioId: String?) {
    context.dataStore.edit { prefs ->
      if (patioId == null) {
        prefs.remove(selectedPatioKey)
      } else {
        prefs[selectedPatioKey] = patioId
      }
    }
  }

  suspend fun saveUserSessionCache(displayName: String?, workspaceId: String?) {
    context.dataStore.edit { prefs ->
      if (!displayName.isNullOrBlank()) {
        prefs[cachedDisplayNameKey] = displayName.trim().take(60)
      }
      if (!workspaceId.isNullOrBlank()) {
        prefs[cachedWorkspaceIdKey] = workspaceId.trim()
      }
    }
  }

  suspend fun getCachedDisplayName(): String? = cachedDisplayName.first()

  suspend fun getCachedWorkspaceId(): String? = cachedWorkspaceId.first()
}
