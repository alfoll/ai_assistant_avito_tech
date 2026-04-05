package com.alfoll.aiassistant.data.local

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_settings")

class ThemePreferenceStorage(private val context: Context) {

    val isDarkThemeFlow: Flow<Boolean> = context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[IS_DARK_THEME] ?: false
        }

    suspend fun setDarkTheme(isDarkTheme: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[IS_DARK_THEME] = isDarkTheme
        }
    }

    companion object {
        private val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }
}