package com.example.niord

import android.content.Context

object UserFlowPreferences {
    private const val PREFS_NAME = "niord_user_flow"
    private const val KEY_DEFAULTS_INITIALIZED = "defaults_initialized"
    private const val KEY_SHOW_CONFIGURATION = "show_configuration"
    private const val KEY_ONBOARDING_AVAILABLE = "onboarding_available"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
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
            .putBoolean(KEY_ONBOARDING_AVAILABLE, false)
            .putBoolean(KEY_ONBOARDING_COMPLETED, false)
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

    fun shouldShowOnboarding(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ONBOARDING_AVAILABLE, false) &&
            !prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingAvailable(context: Context, available: Boolean) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_AVAILABLE, available)
            .apply()
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, completed)
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
