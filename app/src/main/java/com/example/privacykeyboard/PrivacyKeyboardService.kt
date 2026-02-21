package com.example.privacykeyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
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
import com.example.privacykeyboard.data.KeyboardPreferences
import com.example.privacykeyboard.data.KeyboardTheme
import com.example.privacykeyboard.data.UserDictRepository
import com.example.privacykeyboard.databinding.EmojiLayoutBinding
import com.example.privacykeyboard.databinding.KeyboardLayoutBinding
import com.example.privacykeyboard.databinding.KeyboardSpecialBinding
import com.example.privacykeyboard.model.CapsState
import com.example.privacykeyboard.trie.Trie
import com.example.privacykeyboard.trie.loadDictionaryFromAssets
import com.example.privacykeyboard.util.HapticHelper
import com.example.privacykeyboard.util.ThemeHelper
import com.example.privacykeyboard.util.extractCurrentWord
import com.example.privacykeyboard.util.isValidWord

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
    private lateinit var prefs: KeyboardPreferences
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

        // Insert at 0 so emoji scroll sits above keyboardRowsContainer and functionalKeys
        normalBinding.normalContainer.addView(emojiBinding.root, 0)
        normalBinding.keyboardRowsContainer.visibility = View.VISIBLE
        emojiBinding.root.visibility = View.GONE

        activeView = normalBinding.root

        prefs = KeyboardPreferences(this)
        hapticHelper = HapticHelper(this, prefs)
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
            suggestionViews = listOf(
                normalBinding.suggestion1,
                normalBinding.suggestion2,
                normalBinding.suggestion3
            ),
            dividers = listOf(
                normalBinding.suggDivider12,
                normalBinding.suggDivider23
            ),
            onWordSelected = { word, rawInput ->
                currentInputConnection?.deleteSurroundingText(rawInput.length, 0)
                currentInputConnection?.commitText("$word ", 1)
            }
        )

        setupNormalLayout()

        // Emoji toggle button
        normalBinding.btnEmoji.setOnClickListener { toggleEmojiLayout() }

        // Keyboard settings gear (normal keyboard top bar) → opens SettingsActivity
        normalBinding.btnKeyboardSettings.setOnClickListener {
            hapticHelper.perform()
            val intent = android.content.Intent(this, SettingsActivity::class.java)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        // Apply theme to normal keyboard
        ThemeHelper.applyToKeyboard(normalBinding.root, getCurrentTheme())

        setEmojiKeyboardHeight()

        return activeView ?: normalBinding.root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (!restarting) {
            capsController.reset()
        }
        // Sync clipboard/suggestion visibility with the field's current content
        val currentText = currentInputConnection
            ?.getExtractedText(ExtractedTextRequest(), 0)
            ?.text?.toString() ?: ""
        if (currentText.isEmpty()) {
            suggestionController.hide()
            clipboardController.show()
        } else {
            clipboardController.hide()
        }
        updateSettingsButtonVisibility()
    }

    override fun onDestroy() {
        super.onDestroy()
        clipboardController.cleanup()
    }

    /**
     * Called every time the keyboard window becomes visible — including when the user
     * returns from SettingsActivity. Re-applies the current theme so changes take effect
     * immediately without needing to restart the keyboard.
     */
    override fun onWindowShown() {
        super.onWindowShown()
        if (!::normalBinding.isInitialized) return
        val theme = getCurrentTheme()
        ThemeHelper.applyToKeyboard(normalBinding.root, theme)
        if (::specialBinding.isInitialized && isSpecialKeysEnabled) {
            ThemeHelper.applyToKeyboard(specialBinding.root, theme)
        }
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

        if (inputText.isNotEmpty()) {
            clipboardController.hide()
            suggestionController.update(inputText)
        } else {
            suggestionController.hide()
            clipboardController.show()
        }
        updateSettingsButtonVisibility()
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
                    hapticHelper.perform()
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

        // Apostrophe
        normalBinding.functionalKeys.btnApostrophe.setOnClickListener {
            hapticHelper.perform()
            currentInputConnection?.commitText("'", 1)
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
        // Row 2: @ # $ _ & - + ( ) /
        // Row 3: * " ' : ; ! ?  (backspace handled separately below)
        val specialButtons = arrayOf(
            specialBinding.btnAt to "@",
            specialBinding.btnHash to "#",
            specialBinding.btnDollar to "$",
            specialBinding.btnUnderscore to "_",
            specialBinding.btnAmpersand to "&",
            specialBinding.btnMinus to "-",
            specialBinding.btnPlus to "+",
            specialBinding.btnLeftParen to "(",
            specialBinding.btnRightParen to ")",
            specialBinding.btnSlash to "/",
            specialBinding.btnAsterisk to "*",
            specialBinding.btnQuote to "\"",
            specialBinding.btnApostrophe to "'",
            specialBinding.btnColon to ":",
            specialBinding.btnSemicolon to ";",
            specialBinding.btnExclamation to "!",
            specialBinding.btnQuestion to "?"
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

        // Backspace (row 3, above Enter)
        specialBinding.backSpace.btnBackSpace.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    hapticHelper.perform()
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

        // Bottom row — direct IDs (no functionalKeys include)
        specialBinding.btnEnter.setOnClickListener {
            hapticHelper.perform()
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        }

        specialBinding.btnSpace.setOnClickListener {
            hapticHelper.perform()
            currentInputConnection?.commitText(" ", 1)
        }

        specialBinding.btnComma.setOnClickListener {
            hapticHelper.perform()
            currentInputConnection?.commitText(",", 1)
        }

        specialBinding.btnDot.setOnClickListener {
            hapticHelper.perform()
            currentInputConnection?.commitText(".", 1)
        }

        // Toggle back to normal layout
        specialBinding.btnSpecialKeys.setOnClickListener {
            isSpecialKeysEnabled = !isSpecialKeysEnabled
            switchKeyboardLayout(specialBinding.btnSpecialKeys)
        }
    }

    // -----------------------------------------------------------------------
    // Layout switching
    // -----------------------------------------------------------------------

    private fun switchKeyboardLayout(button: Button) {
        activeView = if (isSpecialKeysEnabled) {
            setupSpecialLayout()
            ThemeHelper.applyToKeyboard(specialBinding.root, getCurrentTheme())
            specialBinding.root
        } else {
            setupNormalLayout()
            ThemeHelper.applyToKeyboard(normalBinding.root, getCurrentTheme())
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
            emojiController.render()
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
    // Theme + settings button helpers
    // -----------------------------------------------------------------------

    private fun getCurrentTheme() = KeyboardTheme.forId(prefs.themeId)

    /**
     * Settings gear in the normal keyboard top bar hides when the suggestion
     * strip or clipboard is showing (they need all the horizontal space).
     * In the emoji layout the gear is always visible.
     */
    private fun updateSettingsButtonVisibility() {
        if (!::normalBinding.isInitialized) return
        val anySuggestion =
            normalBinding.suggestion1.visibility == View.VISIBLE ||
            normalBinding.suggestion2.visibility == View.VISIBLE ||
            normalBinding.suggestion3.visibility == View.VISIBLE
        val clipboard = normalBinding.clipboardScroll.visibility == View.VISIBLE
        normalBinding.btnKeyboardSettings.visibility =
            if (anySuggestion || clipboard) View.GONE else View.VISIBLE
    }

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
