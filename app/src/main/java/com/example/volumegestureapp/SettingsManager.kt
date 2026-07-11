package com.example.volumegestureapp

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_THEME = "pref_theme" // "system", "light", "dark"
        
        // Edge Pill Colors
        const val KEY_EDGE_COLOR_A = "edge_color_a"
        const val KEY_EDGE_COLOR_R = "edge_color_r"
        const val KEY_EDGE_COLOR_G = "edge_color_g"
        const val KEY_EDGE_COLOR_B = "edge_color_b"
        
        // Edge Pill Sizes
        const val KEY_EDGE_WIDTH = "edge_width"
        const val KEY_EDGE_HEIGHT = "edge_height"
        
        // Popup Overlay Colors
        const val KEY_POPUP_COLOR_A = "popup_color_a"
        const val KEY_POPUP_COLOR_R = "popup_color_r"
        const val KEY_POPUP_COLOR_G = "popup_color_g"
        const val KEY_POPUP_COLOR_B = "popup_color_b"
        
        // Popup Overlay Sizes
        const val KEY_POPUP_WIDTH = "popup_width"
        const val KEY_POPUP_HEIGHT = "popup_height"
        
        const val KEY_TRIGGER_MODE = "trigger_mode"
        const val KEY_SIDEBAR_LOCKED = "sidebar_locked"
    }

    var theme: String
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var triggerMode: String
        get() = prefs.getString(KEY_TRIGGER_MODE, "long_press") ?: "long_press"
        set(value) = prefs.edit().putString(KEY_TRIGGER_MODE, value).apply()

    var sidebarLocked: Boolean
        get() = prefs.getBoolean(KEY_SIDEBAR_LOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_SIDEBAR_LOCKED, value).apply()

    // Edge Pill Color Defaults (semi-transparent dark)
    var edgeColorA: Int
        get() = prefs.getInt(KEY_EDGE_COLOR_A, 0x26)
        set(value) = prefs.edit().putInt(KEY_EDGE_COLOR_A, value).apply()
    var edgeColorR: Int
        get() = prefs.getInt(KEY_EDGE_COLOR_R, 0x00)
        set(value) = prefs.edit().putInt(KEY_EDGE_COLOR_R, value).apply()
    var edgeColorG: Int
        get() = prefs.getInt(KEY_EDGE_COLOR_G, 0x00)
        set(value) = prefs.edit().putInt(KEY_EDGE_COLOR_G, value).apply()
    var edgeColorB: Int
        get() = prefs.getInt(KEY_EDGE_COLOR_B, 0x00)
        set(value) = prefs.edit().putInt(KEY_EDGE_COLOR_B, value).apply()

    // Edge Pill Size Defaults (width 6dp, height 80dp)
    var edgeWidth: Int
        get() = prefs.getInt(KEY_EDGE_WIDTH, 6)
        set(value) = prefs.edit().putInt(KEY_EDGE_WIDTH, value).apply()
    var edgeHeight: Int
        get() = prefs.getInt(KEY_EDGE_HEIGHT, 80)
        set(value) = prefs.edit().putInt(KEY_EDGE_HEIGHT, value).apply()

    // Popup Color Defaults (semi-transparent light grey/white #CCF5F5F7)
    var popupColorA: Int
        get() = prefs.getInt(KEY_POPUP_COLOR_A, 0xCC)
        set(value) = prefs.edit().putInt(KEY_POPUP_COLOR_A, value).apply()
    var popupColorR: Int
        get() = prefs.getInt(KEY_POPUP_COLOR_R, 0xF5)
        set(value) = prefs.edit().putInt(KEY_POPUP_COLOR_R, value).apply()
    var popupColorG: Int
        get() = prefs.getInt(KEY_POPUP_COLOR_G, 0xF5)
        set(value) = prefs.edit().putInt(KEY_POPUP_COLOR_G, value).apply()
    var popupColorB: Int
        get() = prefs.getInt(KEY_POPUP_COLOR_B, 0xF7)
        set(value) = prefs.edit().putInt(KEY_POPUP_COLOR_B, value).apply()

    // Popup Size Defaults (width 72dp, height 300dp)
    var popupWidth: Int
        get() = prefs.getInt(KEY_POPUP_WIDTH, 72)
        set(value) = prefs.edit().putInt(KEY_POPUP_WIDTH, value).apply()
    var popupHeight: Int
        get() = prefs.getInt(KEY_POPUP_HEIGHT, 300)
        set(value) = prefs.edit().putInt(KEY_POPUP_HEIGHT, value).apply()

    val edgeColorInt: Int
        get() = android.graphics.Color.argb(edgeColorA, edgeColorR, edgeColorG, edgeColorB)

    val popupColorInt: Int
        get() = android.graphics.Color.argb(popupColorA, popupColorR, popupColorG, popupColorB)

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
