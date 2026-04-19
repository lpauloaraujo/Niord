package com.example.niord

import android.content.Context

object UserFlowPreferences {
    private const val PREFS_NAME = "niord_user_flow"
    private const val KEY_DEFAULTS_INITIALIZED = "defaults_initialized"
    private const val KEY_SHOW_CONFIGURATION = "show_configuration"
    private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
    private const val KEY_OVERLAY_LOCKED = "overlay_locked"

    fun ensureDefaults(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DEFAULTS_INITIALIZED, false)) {
            return
        }

        prefs.edit()
            .putBoolean(KEY_DEFAULTS_INITIALIZED, true)
            .putBoolean(KEY_SHOW_CONFIGURATION, false)
            .putBoolean(KEY_OVERLAY_ENABLED, false)
            .putBoolean(KEY_OVERLAY_LOCKED, false)
            .apply()
    }

    fun shouldShowConfiguration(context: Context): Boolean {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_CONFIGURATION, false)
    }

    fun setShowConfiguration(context: Context, shouldShow: Boolean) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_CONFIGURATION, shouldShow)
            .apply()
    }

    fun isOverlayEnabled(context: Context): Boolean {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_OVERLAY_ENABLED, false)
    }

    fun setOverlayEnabled(context: Context, enabled: Boolean) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_OVERLAY_ENABLED, enabled)
            .apply()
    }

    fun isOverlayLocked(context: Context): Boolean {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_OVERLAY_LOCKED, false)
    }

    fun setOverlayLocked(context: Context, locked: Boolean) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_OVERLAY_LOCKED, locked)
            .apply()
    }
}
