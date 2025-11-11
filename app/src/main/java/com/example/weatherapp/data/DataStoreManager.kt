package com.example.weatherapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore by preferencesDataStore(name = "weather_prefs")

object DataStoreManager {
    private val SAVED_LOCATIONS = stringPreferencesKey("saved_locations")
    private val CACHED_WEATHER = stringPreferencesKey("cached_weather")
    private val DEFAULT_LOCATION = stringPreferencesKey("default_location")
    private val DARK_THEME = booleanPreferencesKey("dark_theme")

    suspend fun saveLocations(context: Context, locationsJson: String) {
        context.dataStore.edit { it[SAVED_LOCATIONS] = locationsJson }
    }

    suspend fun loadLocations(context: Context): String {
        return context.dataStore.data.first()[SAVED_LOCATIONS] ?: "[]"
    }

    suspend fun saveCache(context: Context, cacheJson: String) {
        context.dataStore.edit { it[CACHED_WEATHER] = cacheJson }
    }

    suspend fun loadCache(context: Context): String {
        return context.dataStore.data.first()[CACHED_WEATHER] ?: "{}"
    }

    suspend fun saveDefaultLocation(context: Context, locationJson: String) {
        context.dataStore.edit { it[DEFAULT_LOCATION] = locationJson }
    }

    suspend fun loadDefaultLocation(context: Context): String {
        return context.dataStore.data.first()[DEFAULT_LOCATION] ?: ""
    }

    suspend fun saveDarkTheme(context: Context, isDark: Boolean) {
        context.dataStore.edit { it[DARK_THEME] = isDark }
    }

    suspend fun loadDarkTheme(context: Context): Boolean {
        return context.dataStore.data.first()[DARK_THEME] ?: false
    }
}
