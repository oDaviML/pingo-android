package com.pingo.spike.capture

import android.content.Context
import androidx.core.content.edit

class SpikePreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var captureAllPackages: Boolean
        get() = preferences.getBoolean(CAPTURE_ALL_KEY, false)
        set(value) {
            preferences.edit { putBoolean(CAPTURE_ALL_KEY, value) }
        }

    companion object {
        private const val PREFERENCES_NAME = "spike"
        private const val CAPTURE_ALL_KEY = "capture_all_packages"
    }
}
