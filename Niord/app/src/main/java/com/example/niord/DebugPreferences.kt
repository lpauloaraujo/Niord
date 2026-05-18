package com.example.niord

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object DebugPreferences {
    private const val PREFS_NAME = "debug_configurations"
    private const val IS_DEFAULT = "is_default_configurations"
    private const val IS_DEBUG = "is_debug"
    private const val BUILD_DEBUG_VAL = false


    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    fun ensureDefaults(context: Context) {
        val prefs: SharedPreferences = getPrefs(context)

        prefs.edit {
            putBoolean(IS_DEBUG, BUILD_DEBUG_VAL)
        }
    }

    fun isDebug(context: Context): Boolean{
        return getPrefs(context).getBoolean(IS_DEBUG, true);
    }

    fun setDebug(context: Context, isDebug: Boolean) {
        getPrefs(context).edit {
            putBoolean(IS_DEBUG, isDebug)
        }

    }
}