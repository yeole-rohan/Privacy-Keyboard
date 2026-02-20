package com.example.privacykeyboard.controller

import android.content.res.ColorStateList
import android.widget.Button
import androidx.core.content.ContextCompat
import com.example.privacykeyboard.R
import com.example.privacykeyboard.databinding.KeyboardLayoutBinding
import com.example.privacykeyboard.model.CapsState
import com.example.privacykeyboard.util.nextCapsState
import com.example.privacykeyboard.util.shouldAutoOffAfterKey

class CapsController(private val binding: KeyboardLayoutBinding) {

    var state: CapsState = CapsState.SHIFT
        private set

    fun toggle() {
        state = nextCapsState(state)
        updateUI()
    }

    fun reset() {
        state = CapsState.SHIFT
        updateUI()
    }

    fun makeKeysLowercase() {
        if (shouldAutoOffAfterKey(state)) {
            state = CapsState.OFF
            applyToButtons(alphabeticButtons())
            val btn = binding.rowAlphabetic3.capsLock.btnCapsLock
            btn.setBackgroundResource(R.drawable.lower_key_icon)
            btn.backgroundTintList = null
        }
    }

    fun applyToButtons(buttons: Array<Button>) {
        buttons.forEach { button ->
            button.text = if (state != CapsState.OFF)
                button.text.toString().uppercase()
            else
                button.text.toString().lowercase()
        }
    }

    private fun updateUI() {
        val btn = binding.rowAlphabetic3.capsLock.btnCapsLock
        val iconRes = if (state == CapsState.OFF) R.drawable.lower_key_icon else R.drawable.upper_key_icon
        btn.setBackgroundResource(iconRes)
        btn.backgroundTintList = if (state == CapsState.CAPS_LOCK)
            ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, android.R.color.holo_blue_light)
            )
        else null
        applyToButtons(alphabeticButtons())
    }

    private fun alphabeticButtons(): Array<Button> = arrayOf(
        binding.rowAlphabetic1.btnQ, binding.rowAlphabetic1.btnW, binding.rowAlphabetic1.btnE,
        binding.rowAlphabetic1.btnR, binding.rowAlphabetic1.btnT, binding.rowAlphabetic1.btnY,
        binding.rowAlphabetic1.btnU, binding.rowAlphabetic1.btnI, binding.rowAlphabetic1.btnO,
        binding.rowAlphabetic1.btnP, binding.rowAlphabetic2.btnA, binding.rowAlphabetic2.btnS,
        binding.rowAlphabetic2.btnD, binding.rowAlphabetic2.btnF, binding.rowAlphabetic2.btnG,
        binding.rowAlphabetic2.btnH, binding.rowAlphabetic2.btnJ, binding.rowAlphabetic2.btnK,
        binding.rowAlphabetic2.btnL, binding.rowAlphabetic3.btnZ, binding.rowAlphabetic3.btnX,
        binding.rowAlphabetic3.btnC, binding.rowAlphabetic3.btnV, binding.rowAlphabetic3.btnB,
        binding.rowAlphabetic3.btnN, binding.rowAlphabetic3.btnM
    )
}
