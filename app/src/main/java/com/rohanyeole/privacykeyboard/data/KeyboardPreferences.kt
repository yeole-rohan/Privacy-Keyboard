package com.rohanyeole.privacykeyboard.data

import android.content.Context

class KeyboardPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("keyboard_settings", Context.MODE_PRIVATE)

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", true)
        set(value) { prefs.edit().putBoolean("vibration_enabled", value).apply() }

    var vibrationStrength: String   // "light" | "medium" | "strong"
        get() = prefs.getString("vibration_strength", "medium") ?: "medium"
        set(value) { prefs.edit().putString("vibration_strength", value).apply() }

    var themeId: String             // "light" | "dark" | "ocean" | "sunset" | "forest"
        get() = prefs.getString("theme_id", "light") ?: "light"
        set(value) { prefs.edit().putString("theme_id", value).apply() }
}
