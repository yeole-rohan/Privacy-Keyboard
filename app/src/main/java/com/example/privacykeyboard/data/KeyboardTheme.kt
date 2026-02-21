package com.example.privacykeyboard.data

import android.graphics.Color

data class KeyboardTheme(
    val id: String,
    val name: String,
    val keyBg: Int,
    val keyText: Int,
    val kbBg: Int,
    val rippleColor: Int
) {
    companion object {
        val all = listOf(
            KeyboardTheme("light",  "Light",
                Color.WHITE,
                Color.parseColor("#1b1b1d"),
                Color.parseColor("#F2F6F8"),
                Color.parseColor("#CCCCCC")),
            KeyboardTheme("dark",   "Dark",
                Color.parseColor("#3C4043"),
                Color.WHITE,
                Color.parseColor("#202124"),
                Color.parseColor("#666666")),
            KeyboardTheme("ocean",  "Ocean",
                Color.parseColor("#1976D2"),
                Color.WHITE,
                Color.parseColor("#0D47A1"),
                Color.parseColor("#42A5F5")),
            KeyboardTheme("sunset", "Sunset",
                Color.parseColor("#EF6C00"),
                Color.WHITE,
                Color.parseColor("#BF360C"),
                Color.parseColor("#FFAB40")),
            KeyboardTheme("forest", "Forest",
                Color.parseColor("#388E3C"),
                Color.WHITE,
                Color.parseColor("#1B5E20"),
                Color.parseColor("#66BB6A")),
            KeyboardTheme("lavender", "Lavender",
                Color.parseColor("#9575CD"),
                Color.WHITE,
                Color.parseColor("#512DA8"),
                Color.parseColor("#CE93D8")),
            KeyboardTheme("midnight", "Midnight",
                Color.parseColor("#1A1A2E"),
                Color.parseColor("#E0E0E0"),
                Color.parseColor("#000000"),
                Color.parseColor("#3D5AFE")),
        )

        fun forId(id: String) = all.find { it.id == id } ?: all[0]
    }
}
