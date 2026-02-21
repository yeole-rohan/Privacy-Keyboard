package com.rohanyeole.privacykeyboard.util

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.rohanyeole.privacykeyboard.R
import com.rohanyeole.privacykeyboard.data.KeyboardTheme

object ThemeHelper {

    /** Apply a theme to the keyboard root and all key buttons within it. */
    fun applyToKeyboard(root: View, theme: KeyboardTheme) {
        root.setBackgroundColor(theme.kbBg)
        applyToButtons(root, theme)
    }

    private fun applyToButtons(view: View, theme: KeyboardTheme) {
        if (view is Button) {
            if (view.id != R.id.btnEnter) {       // keep the enter arrow drawable
                applyKeyStyle(view, theme)
            }
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyToButtons(view.getChildAt(i), theme)
            }
        }
    }

    fun applyKeyStyle(button: Button, theme: KeyboardTheme) {
        val ctx = button.context
        val radius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 8f, ctx.resources.displayMetrics
        )
        val shape = GradientDrawable().apply {
            setColor(theme.keyBg)
            cornerRadius = radius
        }
        val mask = GradientDrawable().apply {
            setColor(theme.keyBg)
            cornerRadius = radius
        }
        button.background = RippleDrawable(ColorStateList.valueOf(theme.rippleColor), shape, mask)
        button.setTextColor(theme.keyText)
    }
}
