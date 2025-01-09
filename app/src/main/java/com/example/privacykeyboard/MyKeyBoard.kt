package com.example.privacykeyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.privacykeyboard.databinding.KeyboardEmojiBinding
import com.example.privacykeyboard.databinding.KeyboardLayoutBinding
import com.example.privacykeyboard.databinding.KeyboardSpecialBinding

class MyKeyboard : InputMethodService() {
    private var isCapsLockActive: Boolean = true // Tracks Caps Lock state
    private lateinit var normalBinding: KeyboardLayoutBinding // Binding for normal keyboard layout
    private lateinit var specialBinding: KeyboardSpecialBinding // Binding for special keys layout
    private lateinit var emojilBinding: KeyboardEmojiBinding // Binding for emoji keys layout
    private var activeView: View? = null // Currently active view
    private val handler: Handler = Handler(Looper.getMainLooper()) // Handler for backspace repetition
    private var isBackspacePressed: Boolean = false // Tracks if backspace is pressed
    private var isEmojiPressed: Boolean = false // Tracks if emoji is pressed
    private var isSpecialKeysEnabled: Boolean = false // Tracks Special Keys layout state
    private val TAG: String = "MyKeyboard" // Log tag for debugging

    override fun onCreateInputView(): View {
        // Inflate normal and special layouts
        normalBinding = KeyboardLayoutBinding.inflate(layoutInflater)
        specialBinding = KeyboardSpecialBinding.inflate(layoutInflater)

        // Set initial layout to the normal keyboard
        activeView = normalBinding.root
        setupKeyboardLayout(normalBinding)
        return activeView!!
    }

    /**
     * Configures the keyboard layout and its button listeners.
     */
    private fun setupKeyboardLayout(binding: KeyboardLayoutBinding) {
        Log.i(TAG, "setupKeyboardLayout: Configuring normal keyboard layout")
        setupButtonListeners(getNumericButtons()) // Setup numeric buttons
        setupButtonListeners(getAlphabeticButtons()) // Setup alphabetic buttons
        setupBackspaceButton(binding) // Setup backspace button
        setupActionButtons(binding.root) // Setup action buttons like Enter, Space, etc.
        setupToggleSpecialKeysButton(binding) // Setup the toggle button to switch layouts like normal and special keys
        setupToggleEmojiButton(binding) // Setup the toggle button for Emoji
    }

    /**
     * Toggles between the normal keyboard and special keys layouts.
     */
    private fun setupToggleSpecialKeysButton(binding: KeyboardLayoutBinding) {
        val toggleButton = binding.functionalKeys.btnSpecialKeys
        toggleButton.setOnClickListener {
            isSpecialKeysEnabled = !isSpecialKeysEnabled
            switchKeyboardLayout(toggleButton)
        }
    }
    /**
     * Toggles between the emoji, normal keyboard and special keys layouts.
     */
    private fun setupToggleEmojiButton(binding: KeyboardLayoutBinding) {
        val toggleButton = binding.functionalKeys.btnEmoji
        toggleButton.setOnClickListener {
            isEmojiPressed = !isEmojiPressed
            switchKeyboardLayout(toggleButton)
        }
    }

    /**
     * Switches the keyboard layout dynamically based on the isSpecialKeysEnabled flag.
     */
    private fun switchKeyboardLayout(button: Button) {
        Log.i(TAG, "switchKeyboardLayout: Switching layout to ${if (isSpecialKeysEnabled) "special" else "normal"} keyboard")

        activeView = if (isSpecialKeysEnabled) {
            setupKeyboardLayoutForSpecial(specialBinding)
            specialBinding.functionalKeys.btnSpecialKeys.text="abc"
            specialBinding.root
        } else if (isEmojiPressed) {
            setupEmojiBoardLayout(emojilBinding)
            emojilBinding.root
        }else{
            setupKeyboardLayout(normalBinding)
            normalBinding.functionalKeys.btnSpecialKeys.text="!?"
            normalBinding.root
        }

        // Update the input view
        setInputView(activeView)
    }
    /**
     * Configures the Emoji keys layout.
     */
    private fun setupEmojiBoardLayout(emojiBinding: KeyboardEmojiBinding) {
        Log.i(TAG, "setupKeyboardLayoutForSpecial: Configuring special keyboard layout")
        // Array of buttons and their corresponding characters
//        val emojiText = arrayOf(
//            emojiBinding.emojiScrollView. to "\uD83D\uDC36",
//            emojilBinding.emojiAnimal2 to "\uD83D\uDC31",
//            emojilBinding.emojiSmiley1 to "\uD83D\uDE00",
//            emojilBinding.emojiSmiley2 to "\uD83D\uDE01",
//            emojilBinding.emojiSmiley3 to "\uD83D\uDE02"
//        )


        // Setup all buttons in the array
//        setupEmojiButtons(emojiText)

        // Configure Backspace
//        specialBinding.backSpace.btnBackSpace.setOnTouchListener { _, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    isBackspacePressed = true
//                    performSingleBackspace()
//                    startContinuousBackspace()
//                }
//                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                    isBackspacePressed = false
//                    handler.removeCallbacksAndMessages(null) // Stop continuous backspace
//                }
//            }
//            true
//        }
//        setupActionButtons(emojilBinding.root) // Setup action buttons like Enter, Space, etc.

    }
    /**
     * Configures the special keys layout.
     */
    private fun setupKeyboardLayoutForSpecial(specialBinding: KeyboardSpecialBinding) {
        Log.i(TAG, "setupKeyboardLayoutForSpecial: Configuring special keyboard layout")
        // Array of buttons and their corresponding characters
        val specialButtons = arrayOf(
            specialBinding.btnExclamation to "!",
            specialBinding.btnAt to "@",
            specialBinding.btnHash to "#",
            specialBinding.btnDollar to "$",
            specialBinding.btnPercent to "%",
            specialBinding.btnAmpersand to "&",
            specialBinding.btnAsterisk to "*",
            specialBinding.btnLeftParen to "(",
            specialBinding.btnRightParen to ")",
            specialBinding.btnQuestion to "?",
            specialBinding.btnUnderscore to "_",
            specialBinding.btnPlus to "+",
            specialBinding.btnTilde to "~",
            specialBinding.btnColon to ":",
            specialBinding.btnSemicolon to ";",
            specialBinding.btnSlash to "/"
        )
        // Array of numeric buttons and their corresponding characters

        val numericButtons = arrayOf(
            specialBinding.rowNumeric.btn0 to "0",
            specialBinding.rowNumeric.btn1 to "1",
            specialBinding.rowNumeric.btn2 to "2",
            specialBinding.rowNumeric.btn3 to "3",
            specialBinding.rowNumeric.btn4 to "4",
            specialBinding.rowNumeric.btn5 to "5",
            specialBinding.rowNumeric.btn6 to "6",
            specialBinding.rowNumeric.btn7 to "7",
            specialBinding.rowNumeric.btn8 to "8",
            specialBinding.rowNumeric.btn9 to "9"
        )

        // Setup all buttons in the array
        setupSpecialCharacterButtons(specialButtons)
        setupSpecialCharacterButtons(numericButtons)

        // Configure Backspace
        specialBinding.backSpace.btnBackSpace.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isBackspacePressed = true
                    performSingleBackspace()
                    startContinuousBackspace()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isBackspacePressed = false
                    handler.removeCallbacksAndMessages(null) // Stop continuous backspace
                }
            }
            true
        }
        setupActionButtons(specialBinding.root) // Setup action buttons like Enter, Space, etc.

    }
    /**
     * Sets up multiple buttons to commit their corresponding characters when clicked.
     */
    private fun setupSpecialCharacterButtons(buttons: Array<Pair<Button, String>>) {
        buttons.forEach { (button, character) ->
            button.setOnClickListener {
                currentInputConnection?.commitText(character, 1)
            }
        }
    }
    private fun setupEmojiButtons(emojis:Array<Pair<TextView, String>>){
        emojis.forEach { (emoji, character) ->
            emoji.setOnClickListener {
                currentInputConnection?.commitText(character, 1)
            }
        }
    }
    /**
     * Handles the behavior of the backspace button.
     */
    private fun setupBackspaceButton(binding: KeyboardLayoutBinding) {
        binding.rowAlphabetic3.backSpace.btnBackSpace.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isBackspacePressed = true
                    performSingleBackspace()
                    startContinuousBackspace()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isBackspacePressed = false
                    handler.removeCallbacksAndMessages(null) // Stop continuous backspace
                }
            }
            true
        }
    }

    /**
     * Configures action buttons like Enter, Space, Comma, and Dot for both layouts.
     */
    private fun setupActionButtons(view: View) {
        Log.i(TAG, "setupActionButtons: Configuring action buttons")

        val isNormalLayout = view == normalBinding.root

        if (isNormalLayout) {
            // Configure action buttons for the normal layout
            normalBinding.rowAlphabetic3.capsLock.btnCapsLock.setOnClickListener {
                toggleCapsLock()
            }

            normalBinding.functionalKeys.btnEnter.setOnClickListener {
                currentInputConnection?.sendKeyEvent(
                    KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                )
            }

            normalBinding.functionalKeys.btnSpace.setOnClickListener {
                currentInputConnection?.commitText(" ", 1)
                makeKeysLowercase()
            }

            normalBinding.functionalKeys.btnComma.setOnClickListener {
                currentInputConnection?.commitText(",", 1)
                makeKeysLowercase()
            }

            normalBinding.functionalKeys.btnDot.setOnClickListener {
                currentInputConnection?.commitText(".", 1)
                makeKeysLowercase()
            }
        } else {
            // Configure action buttons for the special layout
            specialBinding.functionalKeys.btnEnter.setOnClickListener {
                currentInputConnection?.sendKeyEvent(
                    KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
                )
            }

            specialBinding.functionalKeys.btnSpace.setOnClickListener {
                currentInputConnection?.commitText(" ", 1)
            }

            specialBinding.functionalKeys.btnComma.setOnClickListener {
                currentInputConnection?.commitText(",", 1)
            }

            specialBinding.functionalKeys.btnDot.setOnClickListener {
                currentInputConnection?.commitText(".", 1)
            }

            specialBinding.functionalKeys.btnSpecialKeys.setOnClickListener {
                isSpecialKeysEnabled = !isSpecialKeysEnabled
                Log.i(TAG, "setupToggleSpecialKeysButton: isSpecialKeysEnabled = $isSpecialKeysEnabled")
                switchKeyboardLayout(specialBinding.functionalKeys.btnSpecialKeys)
            }
        }
    }


    /**
     * Toggles the Caps Lock state and updates button text and icons accordingly.
     */
    private fun toggleCapsLock() {
        Log.d(TAG, "toggleCapsLock: Caps Lock state = $isCapsLockActive")
        isCapsLockActive = !isCapsLockActive
        val iconRes = if (isCapsLockActive) R.drawable.capslock_with_filled_background else R.drawable.capslock_with_background
        normalBinding.rowAlphabetic3.capsLock.btnCapsLock.setBackgroundResource(iconRes)
        toggleCapsLockOnButtons(getAlphabeticButtons())
    }

    /**
     * Sends a single backspace key event.
     */
    private fun performSingleBackspace() {
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
    }

    /**
     * Starts continuous backspace operation while the button is held down.
     */
    private fun startContinuousBackspace() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isBackspacePressed) {
                    performSingleBackspace()
                    handler.postDelayed(this, 50) // Repeat backspace every 50ms
                }
            }
        }, 300) // Start after 300ms delay
    }

    /**
     * Returns an array of numeric buttons.
     */
    private fun getNumericButtons(): Array<Button> = arrayOf(
        normalBinding.rowNumeric.btn0, normalBinding.rowNumeric.btn1, normalBinding.rowNumeric.btn2,
        normalBinding.rowNumeric.btn3, normalBinding.rowNumeric.btn4, normalBinding.rowNumeric.btn5,
        normalBinding.rowNumeric.btn6, normalBinding.rowNumeric.btn7, normalBinding.rowNumeric.btn8,
        normalBinding.rowNumeric.btn9
    )

    /**
     * Returns an array of alphabetic buttons from the provided view or current binding.
     */
    private fun getAlphabeticButtons(view: View? = null): Array<Button> {
        val alphabeticView = normalBinding
        return arrayOf(
            alphabeticView.rowAlphabetic1.btnQ, alphabeticView.rowAlphabetic1.btnW, alphabeticView.rowAlphabetic1.btnE,
            alphabeticView.rowAlphabetic1.btnR, alphabeticView.rowAlphabetic1.btnT, alphabeticView.rowAlphabetic1.btnY,
            alphabeticView.rowAlphabetic1.btnU, alphabeticView.rowAlphabetic1.btnI, alphabeticView.rowAlphabetic1.btnO,
            alphabeticView.rowAlphabetic1.btnP, alphabeticView.rowAlphabetic2.btnA, alphabeticView.rowAlphabetic2.btnS,
            alphabeticView.rowAlphabetic2.btnD, alphabeticView.rowAlphabetic2.btnF, alphabeticView.rowAlphabetic2.btnG,
            alphabeticView.rowAlphabetic2.btnH, alphabeticView.rowAlphabetic2.btnJ, alphabeticView.rowAlphabetic2.btnK,
            alphabeticView.rowAlphabetic2.btnL, alphabeticView.rowAlphabetic3.btnZ, alphabeticView.rowAlphabetic3.btnX,
            alphabeticView.rowAlphabetic3.btnC, alphabeticView.rowAlphabetic3.btnV, alphabeticView.rowAlphabetic3.btnB,
            alphabeticView.rowAlphabetic3.btnN, alphabeticView.rowAlphabetic3.btnM
        )
    }

    /**
     * Configures button click listeners for an array of buttons.
     */
    private fun setupButtonListeners(buttons: Array<Button>) {
        Log.i(TAG, "setupButtonListeners: Setting up buttons")
        buttons.forEach { button ->
            button.setOnClickListener {
                val inputText = if (isCapsLockActive) button.text.toString().uppercase() else button.text.toString().lowercase()
                currentInputConnection?.commitText(inputText, 1)
                makeKeysLowercase()
            }
        }
    }

    /**
     * Toggles the Caps Lock state for all provided buttons.
     */
    private fun toggleCapsLockOnButtons(buttons: Array<Button>) {
        buttons.forEach { button ->
            button.text = if (isCapsLockActive) button.text.toString().uppercase() else button.text.toString().lowercase()
        }
    }

    /**
     * Disables Caps Lock and updates button states.
     */
    private fun makeKeysLowercase() {
        if (isCapsLockActive) {
            isCapsLockActive = false
            toggleCapsLockOnButtons(getAlphabeticButtons())
            normalBinding.rowAlphabetic3.capsLock.btnCapsLock.setBackgroundResource(R.drawable.capslock_with_background)
        }
    }
}
