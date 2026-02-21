package com.example.privacykeyboard.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.privacykeyboard.data.KeyboardPreferences

class HapticHelper(context: Context, private val prefs: KeyboardPreferences) {
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun perform() {
        if (!prefs.vibrationEnabled) return
        val duration = when (prefs.vibrationStrength) {
            "light"  -> 15L
            "strong" -> 50L
            else     -> 30L
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}
