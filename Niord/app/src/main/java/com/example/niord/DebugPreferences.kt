package com.example.niord

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object DebugPreferences {
    private const val PREFS_NAME = "debug_configurations"
    private const val IS_DEFAULT = "is_default_configurations"
    private const val IS_DEBUG = "is_debug"


    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    fun ensureDefaults(context: Context) {
        val prefs: SharedPreferences = getPrefs(context)
        if (prefs.getBoolean(IS_DEFAULT, false)){
           return
        }

        prefs.edit {
            putBoolean(IS_DEBUG, true)
            putBoolean(IS_DEFAULT, true)
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