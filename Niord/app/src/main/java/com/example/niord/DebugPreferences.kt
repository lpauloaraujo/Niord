package com.example.niord

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object DebugPreferences {
    private const val PREFS_NAME = "debug_configurations"
    private const val IS_DEBUG = "is_debug"
    private const val BUILD_DEBUG_PREF = "build_debug_value"
    private const val BUILD_DEBUG_VAL = BuildConfig.MOCK_BACKEND


    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    fun ensureDefaults(context: Context) {
        val prefs: SharedPreferences = getPrefs(context)
        val buildValueChanged = !prefs.contains(BUILD_DEBUG_PREF) ||
            prefs.getBoolean(BUILD_DEBUG_PREF, !BUILD_DEBUG_VAL) != BUILD_DEBUG_VAL
        if (prefs.contains(IS_DEBUG) && !buildValueChanged) return

        prefs.edit {
            putBoolean(IS_DEBUG, BUILD_DEBUG_VAL)
            putBoolean(BUILD_DEBUG_PREF, BUILD_DEBUG_VAL)
        }
    }

    fun isDebug(context: Context): Boolean{
        return getPrefs(context).getBoolean(IS_DEBUG, BUILD_DEBUG_VAL)
    }

    fun setDebug(context: Context, isDebug: Boolean) {
        getPrefs(context).edit {
            putBoolean(IS_DEBUG, isDebug)
        }

    }
}
