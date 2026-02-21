package com.rohanyeole.privacykeyboard.controller

import android.graphics.Typeface
import android.widget.Button
import com.rohanyeole.privacykeyboard.databinding.KeyboardLayoutBinding
import com.rohanyeole.privacykeyboard.model.CapsState
import com.rohanyeole.privacykeyboard.util.nextCapsState
import com.rohanyeole.privacykeyboard.util.shouldAutoOffAfterKey

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
            applyOffState(btn)
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
        when (state) {
            CapsState.OFF -> applyOffState(btn)
            CapsState.SHIFT -> {
                btn.text = "A"
                btn.setTypeface(null, Typeface.NORMAL)
                btn.paintFlags = btn.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
            }
            CapsState.CAPS_LOCK -> {
                btn.text = "A"
                btn.setTypeface(null, Typeface.BOLD)
                btn.paintFlags = btn.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            }
        }
        applyToButtons(alphabeticButtons())
    }

    private fun applyOffState(btn: Button) {
        btn.text = "a"
        btn.setTypeface(null, Typeface.NORMAL)
        btn.paintFlags = btn.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
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
