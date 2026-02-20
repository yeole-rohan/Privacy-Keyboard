package com.example.privacykeyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.widget.Button
import com.example.privacykeyboard.controller.CapsController
import com.example.privacykeyboard.controller.ClipboardController
import com.example.privacykeyboard.controller.EmojiController
import com.example.privacykeyboard.controller.SuggestionController
import com.example.privacykeyboard.data.EmojiRepository
import com.example.privacykeyboard.data.UserDictRepository
import com.example.privacykeyboard.databinding.EmojiLayoutBinding
import com.example.privacykeyboard.databinding.KeyboardLayoutBinding
import com.example.privacykeyboard.databinding.KeyboardSpecialBinding
import com.example.privacykeyboard.model.CapsState
import com.example.privacykeyboard.trie.Trie
import com.example.privacykeyboard.trie.loadDictionaryFromAssets
import com.example.privacykeyboard.util.HapticHelper
import com.example.privacykeyboard.util.extractCurrentWord
import com.example.privacykeyboard.util.isValidWord
import android.os.Handler
import android.os.Looper

class PrivacyKeyboardService : InputMethodService() {

    private val TAG = "PrivacyKeyboard"

    // View bindings
    private lateinit var normalBinding: KeyboardLayoutBinding
    private lateinit var emojiBinding: EmojiLayoutBinding
    private lateinit var specialBinding: KeyboardSpecialBinding

    private var activeView: View? = null

    // Handler for continuous backspace
    private val handler = Handler(Looper.getMainLooper())
    private var isBackspacePressed = false
    private var isSpecialKeysEnabled = false

    // Helpers / repositories
    private lateinit var hapticHelper: HapticHelper
    private lateinit var emojiRepo: EmojiRepository
    private lateinit var userDictRepo: UserDictRepository
    private lateinit var trie: Trie

    // Controllers
    private lateinit var capsController: CapsController
    private lateinit var clipboardController: ClipboardController
    private lateinit var emojiController: EmojiController
    private lateinit var suggestionController: SuggestionController

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    override fun onCreateInputView(): View {
        normalBinding = KeyboardLayoutBinding.inflate(layoutInflater)
        specialBinding = KeyboardSpecialBinding.inflate(layoutInflater)
        emojiBinding = EmojiLayoutBinding.inflate(layoutInflater)

        normalBinding.normalContainer.addView(emojiBinding.root)
        normalBinding.keyboardRowsContainer.visibility = View.VISIBLE
        emojiBinding.root.visibility = View.GONE

        activeView = normalBinding.root

        hapticHelper = HapticHelper(this)
        emojiRepo = EmojiRepository(this)
        userDictRepo = UserDictRepository(this)

        initTrie()

        capsController = CapsController(normalBinding)

        clipboardController = ClipboardController(
            context = this,
            clipboardScroll = normalBinding.clipboardScroll,
            clipboardContainer = normalBinding.clipboardContainer,
            onTextSelected = { text -> currentInputConnection?.commitText(text, 1) }
        )
        clipboardController.setup()

        emojiController = EmojiController(
            context = this,
            emojiBinding = emojiBinding,
            emojiRepo = emojiRepo,
            onEmojiSelected = { emoji ->
                hapticHelper.perform()
                currentInputConnection?.commitText("$emoji ", 1)
            }
        )
        emojiController.setup()

        suggestionController = SuggestionController(
            trie = trie,
            userDictRepo = userDictRepo,
            autocompleteArea = normalBinding.autocompleteArea,
            suggestionViews = listOf(
                normalBinding.suggestion1,
                normalBinding.suggestion2,
                normalBinding.suggestion3
            ),
            onWordSelected = { word, rawInput ->
                currentInputConnection?.deleteSurroundingText(rawInput.length, 0)
                currentInputConnection?.commitText("$word ", 1)
            }
        )

        setupNormalLayout()

        // Emoji toggle button
        normalBinding.btnEmoji.setOnClickListener { toggleEmojiLayout() }

        // Emoji backspace (inside emoji picker)
        emojiBinding.btnBackspace.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isBackspacePressed = true
                    performSingleBackspace()
                    startContinuousBackspace()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isBackspacePressed = false
                    handler.removeCallbacksAndMessages(null)
                }
            }
            true
        }

        setEmojiKeyboardHeight()

        return activeView ?: normalBinding.root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!restarting) {
            capsController.reset()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardController.cleanup()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd
        )

        val inputText = currentInputConnection
            ?.getExtractedText(ExtractedTextRequest(), 0)
            ?.text?.toString() ?: ""

        val clipboardScroll = normalBinding.clipboardScroll
        val clipboardContainer = normalBinding.clipboardContainer

        if (inputText.isNotEmpty()) {
            clipboardContainer.visibility = View.GONE
            clipboardScroll.visibility = View.GONE
            suggestionController.update(inputText)
        } else {
            suggestionController.hide()
            clipboardContainer.visibility = View.VISIBLE
            clipboardScroll.visibility = View.VISIBLE
        }
    }

    // -----------------------------------------------------------------------
    // Normal keyboard setup
    // -----------------------------------------------------------------------

    private fun setupNormalLayout() {
        // Alphabetic and numeric button listeners
        setupButtonListeners(numericButtons())
        setupButtonListeners(alphabeticButtons())

        // Caps lock
        normalBinding.rowAlphabetic3.capsLock.btnCapsLock.setOnClickListener {
            hapticHelper.perform()
            capsController.toggle()
        }

        // Backspace
        normalBinding.rowAlphabetic3.backSpace.btnBackSpace.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isBackspacePressed = true
                    performSingleBackspace()
                    startContinuousBackspace()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isBackspacePressed = false
                    handler.removeCallbacksAndMessages(null)
                }
            }
            true
        }

        // Enter
        normalBinding.functionalKeys.btnEnter.setOnClickListener {
            hapticHelper.perform()
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        }

        // Space — click commits space; swipe left/right moves cursor
        var swipeStartX = 0f
        normalBinding.functionalKeys.btnSpace.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    swipeStartX = event.x
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - swipeStartX
                    val threshold = 30f
                    val steps = (dx / threshold).toInt()
                    if (steps != 0) {
                        val keyCode = if (steps > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
                        repeat(Math.abs(steps)) {
                            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                        }
                        swipeStartX += steps * threshold
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick()
                    true
                }
                else -> false
            }
        }
        normalBinding.functionalKeys.btnSpace.setOnClickListener {
            hapticHelper.perform()
            currentInputConnection?.commitText(" ", 1)
            capsController.makeKeysLowercase()
            val currentWord = getCurrentWord()
            if (isValidWord(currentWord)) {
                userDictRepo.save(currentWord)
            }
        }

        // Comma
        normalBinding.functionalKeys.btnComma.setOnClickListener {
            hapticHelper.perform()
            currentInputConnection?.commitText(",", 1)
            capsController.makeKeysLowercase()
        }

        // Dot
        normalBinding.functionalKeys.btnDot.setOnClickListener {
            hapticHelper.perform()
            currentInputConnection?.commitText(".", 1)
            capsController.makeKeysLowercase()
        }

        // Toggle to special layout
        normalBinding.functionalKeys.btnSpecialKeys.text = "!?"
        normalBinding.functionalKeys.btnSpecialKeys.setOnClickListener {
            isSpecialKeysEnabled = !isSpecialKeysEnabled
            switchKeyboardLayout(normalBinding.functionalKeys.btnSpecialKeys)
        }
    }

    // -----------------------------------------------------------------------
    // Special keyboard setup
    // -----------------------------------------------------------------------

    private fun setupSpecialLayout() {
        val specialButtons = arrayOf(
            specialBinding.btnMinus to "-",
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

        setupCharacterButtons(specialButtons)
        setupCharacterButtons(numericButtons)

        // Backspace
        specialBinding.backSpace.btnBackSpace.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isBackspacePressed = true
                    performSingleBackspace()
                    startContinuousBackspace()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isBackspacePressed = false
                    handler.removeCallbacksAndMessages(null)
                }
            }
            true
        }

        // Enter
        specialBinding.functionalKeys.btnEnter.setOnClickListener {
            hapticHelper.perform()
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        }

        // Space
        specialBinding.functionalKeys.btnSpace.setOnClickListener {
            hapticHelper.perform()
            currentInputConnection?.commitText(" ", 1)
        }

        // Comma
        specialBinding.functionalKeys.btnComma.setOnClickListener {
            hapticHelper.perform()
            currentInputConnection?.commitText(",", 1)
        }

        // Dot
        specialBinding.functionalKeys.btnDot.setOnClickListener {
            hapticHelper.perform()
            currentInputConnection?.commitText(".", 1)
        }

        // Toggle back to normal layout
        specialBinding.functionalKeys.btnSpecialKeys.text = getString(R.string.normal_mode_text)
        specialBinding.functionalKeys.btnSpecialKeys.setOnClickListener {
            isSpecialKeysEnabled = !isSpecialKeysEnabled
            switchKeyboardLayout(specialBinding.functionalKeys.btnSpecialKeys)
        }
    }

    // -----------------------------------------------------------------------
    // Layout switching
    // -----------------------------------------------------------------------

    private fun switchKeyboardLayout(button: Button) {
        activeView = if (isSpecialKeysEnabled) {
            setupSpecialLayout()
            specialBinding.root
        } else {
            setupNormalLayout()
            normalBinding.root
        }
        setInputView(activeView)
    }

    // -----------------------------------------------------------------------
    // Emoji layout toggle
    // -----------------------------------------------------------------------

    private fun toggleEmojiLayout() {
        if (emojiBinding.root.visibility == View.VISIBLE) {
            emojiBinding.root.visibility = View.GONE
            normalBinding.keyboardRowsContainer.visibility = View.VISIBLE
        } else {
            normalBinding.keyboardRowsContainer.visibility = View.GONE
            emojiBinding.root.visibility = View.VISIBLE
            emojiController.populateRecent()
        }
    }

    private fun setEmojiKeyboardHeight() {
        val screenHeight = resources.displayMetrics.heightPixels
        emojiBinding.scrollView.layoutParams.height = (screenHeight * 0.3).toInt()
    }

    // -----------------------------------------------------------------------
    // Trie initialization
    // -----------------------------------------------------------------------

    private fun initTrie() {
        trie = Trie()
        loadDictionaryFromAssets(this).forEach { word ->
            trie.insert(word)
        }
    }

    // -----------------------------------------------------------------------
    // Button wiring helpers
    // -----------------------------------------------------------------------

    private fun setupButtonListeners(buttons: Array<Button>) {
        Log.i(TAG, "setupButtonListeners: Setting up buttons")
        buttons.forEach { button ->
            button.setOnClickListener {
                hapticHelper.perform()
                val inputText = if (capsController.state != CapsState.OFF)
                    button.text.toString().uppercase()
                else
                    button.text.toString().lowercase()
                currentInputConnection?.commitText(inputText, 1)
                capsController.makeKeysLowercase()
            }
        }
    }

    private fun setupCharacterButtons(buttons: Array<Pair<Button, String>>) {
        buttons.forEach { (button, character) ->
            button.setOnClickListener {
                hapticHelper.perform()
                currentInputConnection?.commitText(character, 1)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Backspace helpers
    // -----------------------------------------------------------------------

    private fun performSingleBackspace() {
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
    }

    private fun startContinuousBackspace() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isBackspacePressed) {
                    performSingleBackspace()
                    handler.postDelayed(this, 30)
                }
            }
        }, 200)
    }

    // -----------------------------------------------------------------------
    // Button accessors
    // -----------------------------------------------------------------------

    private fun alphabeticButtons(): Array<Button> = arrayOf(
        normalBinding.rowAlphabetic1.btnQ, normalBinding.rowAlphabetic1.btnW,
        normalBinding.rowAlphabetic1.btnE, normalBinding.rowAlphabetic1.btnR,
        normalBinding.rowAlphabetic1.btnT, normalBinding.rowAlphabetic1.btnY,
        normalBinding.rowAlphabetic1.btnU, normalBinding.rowAlphabetic1.btnI,
        normalBinding.rowAlphabetic1.btnO, normalBinding.rowAlphabetic1.btnP,
        normalBinding.rowAlphabetic2.btnA, normalBinding.rowAlphabetic2.btnS,
        normalBinding.rowAlphabetic2.btnD, normalBinding.rowAlphabetic2.btnF,
        normalBinding.rowAlphabetic2.btnG, normalBinding.rowAlphabetic2.btnH,
        normalBinding.rowAlphabetic2.btnJ, normalBinding.rowAlphabetic2.btnK,
        normalBinding.rowAlphabetic2.btnL, normalBinding.rowAlphabetic3.btnZ,
        normalBinding.rowAlphabetic3.btnX, normalBinding.rowAlphabetic3.btnC,
        normalBinding.rowAlphabetic3.btnV, normalBinding.rowAlphabetic3.btnB,
        normalBinding.rowAlphabetic3.btnN, normalBinding.rowAlphabetic3.btnM
    )

    private fun numericButtons(): Array<Button> = arrayOf(
        normalBinding.rowNumeric.btn0, normalBinding.rowNumeric.btn1,
        normalBinding.rowNumeric.btn2, normalBinding.rowNumeric.btn3,
        normalBinding.rowNumeric.btn4, normalBinding.rowNumeric.btn5,
        normalBinding.rowNumeric.btn6, normalBinding.rowNumeric.btn7,
        normalBinding.rowNumeric.btn8, normalBinding.rowNumeric.btn9
    )

    // -----------------------------------------------------------------------
    // Word helper
    // -----------------------------------------------------------------------

    private fun getCurrentWord(): String {
        val textBeforeCursor = currentInputConnection
            ?.getTextBeforeCursor(100, 0)?.toString() ?: ""
        Log.i(TAG, "current word: $textBeforeCursor")
        return extractCurrentWord(textBeforeCursor)
    }
}
