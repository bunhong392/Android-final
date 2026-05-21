package com.shopapp.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {

    private const val PREF_NAME = "shopapp_prefs"
    private const val KEY_DARK  = "dark_mode"

    fun isDarkMode(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK, false)
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_DARK, enabled).apply()
        applyTheme(enabled)
    }

    fun applyTheme(enabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES
            else         AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /** Call from Application.onCreate() to restore saved preference */
    fun applySavedTheme(context: Context) {
        applyTheme(isDarkMode(context))
    }
}
