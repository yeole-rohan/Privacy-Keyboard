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
// Core Android components
import android.content.Context
// For clipboard functionality
import android.content.ClipboardManager
import android.content.ClipData
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.ExtractedTextRequest

// For UI components like GridLayout and HorizontalScrollView
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.GridLayout
import android.widget.ImageButton

// For Input Connection updates
import android.view.inputmethod.InputConnection
import android.widget.ScrollView
import com.example.privacykeyboard.databinding.EmojiLayoutBinding
import com.example.privacykeyboard.databinding.TopLayoutBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.util.zip.ZipInputStream


class MyKeyboard : InputMethodService() {
    // Constant for logging, renamed for clarity and following convention
    private val TAG: String = "MyKeyboard"

    // Boolean flag for Caps Lock state, use `val` if it's not changing after initialization
    private var isCapsLockActive: Boolean = true

    // Binding variables initialized using lateinit
    private lateinit var normalBinding: KeyboardLayoutBinding
    private lateinit var emojiBinding: EmojiLayoutBinding
    private lateinit var specialBinding: KeyboardSpecialBinding

    // Active view, nullable to handle no active view scenario
    private var activeView: View? = null

    // Handler for main thread tasks, initialized with Looper.getMainLooper()
    private val handler: Handler = Handler(Looper.getMainLooper())

    // Boolean flags for Backspace state and Special Keys layout
    private var isBackspacePressed: Boolean = false
    private var isSpecialKeysEnabled: Boolean = false

    // UI container for keyboard rows, initialized via binding
    private lateinit var keyboardRowsContainer: LinearLayout

    // Trie for word suggestions or any similar purpose, initialized on setup
    private lateinit var trie: Trie

    override fun onCreateInputView(): View {
        // Inflate keyboard layouts using View Binding
        normalBinding = KeyboardLayoutBinding.inflate(layoutInflater)
        specialBinding = KeyboardSpecialBinding.inflate(layoutInflater)
        emojiBinding = EmojiLayoutBinding.inflate(layoutInflater)

        // Add emoji layout to the normal layout container
        normalBinding.normalContainer.addView(emojiBinding.root)

        // Initialize keyboard rows container and set initial visibility
        keyboardRowsContainer = normalBinding.keyboardRowsContainer
        keyboardRowsContainer.visibility = View.VISIBLE

        // Initially hide the emoji layout
        emojiBinding.root.visibility = View.GONE

        // Set initial layout to normal keyboard view
        activeView = normalBinding.root
        setupKeyboardLayout(normalBinding)

        // Configure emoji button click listener
        normalBinding.btnEmoji.setOnClickListener { toggleEmojiLayout() }

        // Set the height of the emoji keyboard ScrollView dynamically
        setEmojiKeyboardHeight()

        // Initialize the Trie with dictionary data
        initializeTrie(this)

        // Return the current active view (the normal keyboard layout)
        return activeView!!
    }

    /**
     * Sets the height of the emoji keyboard ScrollView dynamically based on screen height.
     */
    private fun setEmojiKeyboardHeight() {
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

        // Set keyboard height to approximately 30% of screen height (adjust factor if needed)
        val keyboardHeight = (screenHeight * 0.3).toInt()
        emojiBinding.scrollView.layoutParams.height = keyboardHeight
    }

    override fun onDestroy() {
        super.onDestroy()
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.removePrimaryClipChangedListener { } // Unregister the listener
    }

    private fun initializeTrie(context: Context) {
        trie = Trie() // Create a new Trie instance
        val wordList = loadDictionaryFromAssets(context) // Load the dictionary
        wordList.forEach { word -> trie.insert(word) } // Insert words into the Trie
        Log.i(TAG, "Trie initialized with ${wordList.size} words")
    }
    /**
     * Show the keyboard rows.
     */
    private fun showKeyboardRows() {
        keyboardRowsContainer.visibility = View.VISIBLE
    }
    /**
     * Hide the keyboard rows.
     */
    private fun hideKeyboardRows() {
        keyboardRowsContainer.visibility = View.GONE
    }
    private fun toggleEmojiLayout() {
        Log.i(TAG, "toggleEmojiLayout: clicked")
        if (emojiBinding.root.visibility == View.VISIBLE) {
            // Hide emoji layout
            emojiBinding.root.visibility = View.GONE
            showKeyboardRows()
        } else {
            hideKeyboardRows()
            // Show emoji layout
            emojiBinding.root.visibility = View.VISIBLE
            setupEmojiPicker()
        }
    }

    private fun setupEmojiPicker() {
        val emojisByRecentCategory = mutableMapOf("Recent" to mutableListOf<String>())

        val emojisByCategory = mapOf(
            // Smileys & Emotion
            "Smileys" to listOf(
                "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆", "😉",
                "😊", "😋", "😎", "😍", "😘", "😗", "😙", "😚", "🙂",
                "🤗", "🤩", "🤔", "🤨", "😐", "😑", "😶", "🙄", "😏",
                "😣", "😥", "😮", "🤐", "😯", "😪", "😫", "😴", "😌",
                "😛", "😜", "😝", "🤤", "😒", "😓", "😔", "😕", "🙃",
                "🫠", "😖", "😷", "🤒", "🤕", "🤢", "🤮", "🤧", "😇",
                "🥳", "🥺", "🤠", "🤡", "🤥", "🤫", "🤭", "🫢", "🫣",
                "🫡", "🤓", "🧐", "👋", "💅", "🤳", "💪", "🦾", "🦵",
                "🤚", "🖐️", "✋", "🖖", "🫱", "🫲", "🫳", "🫴", "👌",
                "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈",
                "👉", "👆", "🖕", "👇", "☝️", "🫵", "👍", "👎", "✊",
                "👊", "🤛", "🤜", "👏", "🫶", "👐", "🤲", "🙌", "🫡",
                "🫢", "🫣", "🙏","😈", "👿", "👹", "👺", "💀", "☠️",
                "👻", "👽", "👾", "🤖", "😺", "😸", "😹", "😻", "😼",
                "😽", "🙀", "😿", "😾", "🙈", "🙉", "🙊", "💋", "💌",
                "💘", "💝", "💖", "💗", "💓", "💞", "💕", "💟", "❣️",
                "💔", "❤️", "🩷", "🧡", "💛", "💚", "💙", "🩵", "💜",
                "🤎", "🖤", "🩶", "🤍", "💯", "💢", "💥", "💫", "💦",
                "💨", "🕳️", "💣", "💬", "🗨️", "🗯️", "💭", "💤",  "✍️",
                "🦿", "🦶", "👂", "🦻", "👃", "🧠", "🫀", "🫁", "🦷",
                "🦴", "👀", "🫦", "👁️", "👅", "👄", "🫤", "🫠", "🫡",
                "🧖‍♀️", "🧖‍♂️", "🧖", "👩‍🦰", "👨‍🦰", "👩‍🦱", "👨‍🦱", "👩‍🦳", "👨‍🦳",
                "👩‍🦲", "👨‍🦲", "👱‍♀️", "👱‍♂️", "👴", "👵", "👲", "👳‍♀️", "👳‍♂️",
                "🧕", "👮‍♀️", "👮‍♂️", "👷‍♀️", "👷‍♂️", "💂‍♀️", "💂‍♂️", "🕵️‍♀️", "🕵️‍♂️",
                "👩‍⚕️", "👨‍⚕️", "👩‍🌾", "👨‍🌾", "👩‍🍳", "👨‍🍳", "👩‍🎓", "👨‍🎓", "👩‍🎤",
                "👨‍🎤", "👩‍🏫", "👨‍🏫", "👩‍🏭", "👨‍🏭", "👩‍💻", "👨‍💻", "👩‍💼", "👨‍💼",
                "👩‍🔧", "👨‍🔧", "👩‍🔬", "👨‍🔬", "👩‍🎨", "👨‍🎨", "👩‍🚒", "👨‍🚒", "👩‍✈️",
                "👨‍✈️", "👩‍🚀", "👨‍🚀", "👩‍⚖️", "👨‍⚖️", "👰‍♀️", "👰‍♂️", "🤵‍♀️", "🤵‍♂️",
                "👸", "🤴", "🫅", "🫆", "🦸‍♀️", "🦸‍♂️", "🦹‍♀️", "🦹‍♂️", "🧙‍♀️",
                "🧙‍♂️", "🧝‍♀️", "🧝‍♂️", "🧛‍♀️", "🧛‍♂️", "🧟‍♀️", "🧟‍♂️", "🧞‍♀️", "🧞‍♂️",
                "🧜‍♀️", "🧜‍♂️", "🧚‍♀️", "🧚‍♂️", "👼", "🎅", "🤶", "🧑‍🎄", "🦸",
                "🦹", "🧙", "🧝", "🧛", "🧟", "🧞", "🧜", "🧚", "👯",
                "👯‍♂️", "👯‍♀️", "💃", "🕺", "🕴️", "👩‍🦽", "👨‍🦽", "🧑‍🦼", "👨‍🦼",
                "👩‍🦼", "🧍", "🧍‍♂️", "🧍‍♀️", "🧎", "🧎‍♂️", "🧎‍♀️", "👫", "👭",
                "👬", "💏", "👩‍❤️‍💋‍👨", "👩‍❤️‍💋‍👩", "👨‍❤️‍💋‍👨", "💑", "👩‍❤️‍👨", "👩‍❤️‍👩", "👨‍❤️‍👨",
                "👪", "👩‍👩‍👧", "👩‍👩‍👧‍👦", "👩‍👩‍👦", "👩‍👩‍👦‍👦", "👩‍👩‍👧‍👧", "👨‍👨‍👧", "👨‍👨‍👧‍👦", "👨‍👨‍👦",
                "👨‍👨‍👦‍👦", "👨‍👨‍👧‍👧", "👩‍👦", "👩‍👧", "👩‍👧‍👦", "👩‍👦‍👦", "👩‍👧‍👧", "👨‍👦", "👨‍👧",
                "👨‍👧‍👦", "👨‍👦‍👦", "👨‍👧‍👧", "🧑‍🤝‍🧑", "👭", "👬", "👫"
            ),

            // Animals & Nature
            "Animals" to listOf(
                "🐵", "🐒", "🦍", "🦧", "🐶", "🐕", "🦮", "🐕‍🦺", "🐩",
                "🐺", "🦊", "🦝", "🐱", "🐈", "🐈‍⬛", "🦁", "🐯", "🐅",
                "🐆", "🐴", "🐎", "🦄", "🫎", "🫏", "🐮", "🐂", "🐃",
                "🐄", "🐷", "🐖", "🐗", "🐽", "🐏", "🐑", "🐐", "🐪",
                "🐫", "🦙", "🦒", "🦘", "🦥", "🦦", "🦨", "🦡", "🐘",
                "🦣", "🐭", "🐁", "🐀", "🐹", "🐰", "🐇", "🐿️", "🦫",
                "🦔", "🦇", "🐻", "🐻‍❄️", "🐨", "🐼", "🦥", "🦦", "🦨",
                "🐾", "🦃", "🐔", "🐓", "🐣", "🐤", "🐥", "🐦", "🐧",
                "🕊️", "🦅", "🦆", "🦢", "🦉", "🦤", "🪶", "🦩", "🦚",
                "🦜", "🐸", "🐊", "🐢", "🦎", "🐍", "🐲", "🐉", "🦕",
                "🦖", "🐋", "🐳", "🐬", "🦭", "🐟", "🐠", "🐡", "🦈",
                "🐙", "🪼", "🐚", "🪸", "🐌", "🦋", "🐛", "🐜", "🐝",
                "🪲", "🐞", "🦗", "🪳", "🪰", "🪱", "🦟", "🦠", "💐",
                "🌸", "💮", "🪷", "🌹", "🥀", "🌺", "🌻", "🌼", "🌷",
                "🌱", "🪴", "🌿", "☘️", "🍀", "🎍", "🪹", "🪵", "🍂",
                "🍁", "🍄", "🐚", "🌾", "💧", "💦", "🌊", "🌫️", "☂️",
                "☔", "🌧️", "⛈️", "🌩️", "🌨️", "☃️", "❄️", "🌬️", "💨",
                "🌪️", "🌈", "☀️", "🌤️", "⛅", "🌥️", "☁️", "🌦️", "🌤",
                "🌩", "🌪", "🌀", "🌙", "🌑", "🌒", "🌓", "🌔", "🌕",
                "🌖", "🌗", "🌘", "🌚", "🌛", "🌜", "☀️", "⭐", "🌟",
                "🌠", "🌌", "☄️", "🪐", "🌍", "🌎", "🌏", "🌐", "🗺️",
                "🪨", "🪵", "⛰️", "🏔️", "🗻", "🌋", "🗾", "🏕️", "🏞️",
                "🛤️", "🌅", "🌄", "🌇", "🌆", "🌉", "🌌", "🎑", "🗾",
                "🌍", "🌎", "🌏"
            ),

            // Food & Drink
            "Food" to listOf(
                "🍇", "🍈", "🍉", "🍊", "🍋", "🍌", "🍍", "🥭", "🍎",
                "🍏", "🍐", "🍑", "🍒", "🍓", "🫐", "🥝", "🍅", "🫒",
                "🥥", "🥑", "🍆", "🥔", "🥕", "🌽", "🌶️", "🫑", "🥒",
                "🥬", "🥦", "🧄", "🧅", "🍄", "🥜", "🫘", "🌰", "🍞",
                "🥐", "🥖", "🫓", "🥨", "🥯", "🥞", "🧇", "🧀", "🍖",
                "🍗", "🥩", "🥓", "🍔", "🍟", "🍕", "🌭", "🥪", "🌮",
                "🌯", "🫔", "🥙", "🧆", "🥗", "🥘", "🫕", "🍲", "🍛",
                "🍣", "🍱", "🥟", "🦪", "🍤", "🍙", "🍚", "🍘", "🍥",
                "🥠", "🥮", "🍢", "🍡", "🍧", "🍨", "🍦", "🥧", "🧁",
                "🍰", "🎂", "🍮", "🍭", "🍬", "🍫", "🍿", "🧈", "🧂",
                "🥤", "🧋", "🧃", "🧉", "🍼", "☕", "🍵", "🫖", "🍶",
                "🍾", "🍷", "🍸", "🍹", "🍺", "🍻", "🥂", "🥃", "🫗",
                "🥄", "🍴", "🍽️", "🥢", "🧂", "🧊", "🫙", "🧇", "🧈",
                "🫕", "🧊"
            ),

            // Travel & Places
            "Travel" to listOf(
                "🌍", "🌎", "🌏", "🗺️", "🗾", "🏔️", "⛰️", "🌋", "🗻",
                "🏕️", "🏖️", "🏜️", "🏝️", "🏞️", "🏟️", "🏛️", "🏗️", "🪨",
                "🛖", "🏘️", "🏚️", "🏠", "🏡", "🏢", "🏣", "🏤", "🏥",
                "🏦", "🏨", "🏩", "🏪", "🏫", "🏬", "🏭", "🏯", "🏰",
                "💒", "🗼", "🗽", "⛪", "🕌", "🛕", "🕍", "⛩️", "🕋",
                "⛲", "⛺", "🌁", "🌃", "🏙️", "🌄", "🌅", "🌆", "🌇",
                "🌉", "♨️", "🎠", "🎡", "🎢", "💈", "🎪", "🚂", "🚃",
                "🚄", "🚅", "🚆", "🚇", "🚈", "🚉", "🚊", "🚝", "🚞",
                "🚋", "🚌", "🚍", "🚎", "🚐", "🚑", "🚒", "🚓", "🚔",
                "🚕", "🚖", "🚗", "🚘", "🚙", "🛻", "🚚", "🚛", "🚜",
                "🏎️", "🏍️", "🛵", "🦽", "🦼", "🛺", "🚲", "🛴", "🛹",
                "🛼", "🚏", "🛣️", "🛤️", "🛞", "⛽", "🚨", "🚥", "🚦",
                "🛑", "🚧", "⚓", "⛵", "🛶", "🚤", "🛳️", "⛴️", "🛥️",
                "🚢", "✈️", "🛩️", "🛫", "🛬", "🪂", "💺", "🚁", "🚟",
                "🚠", "🚡", "🛰️", "🚀", "🛸", "🛎️", "🧳"
            ),

            // Activities
            "Activities" to listOf(
                "🎃", "🎄", "🎆", "🎇", "🧨", "✨", "🎈", "🎉", "🎊",
                "🎋", "🎍", "🎎", "🎏", "🎐", "🎑", "🧧", "🎀", "🎁",
                "🎗️", "🎟️", "🎫", "🎖️", "🏆", "🏅", "🥇", "🥈", "🥉",
                "⚽", "⚾", "🥎", "🏀", "🏐", "🏈", "🏉", "🎾", "🥏",
                "🎳", "🏏", "🏑", "🏒", "🥍", "🏓", "🏸", "🥊", "🥋",
                "🥅", "⛳", "⛸️", "🎣", "🤿", "🎽", "🎿", "🛷", "🥌",
                "🎯", "🪀", "🪁", "🎱", "🔮", "🪄", "🧿", "🪬", "🎮",
                "🕹️", "🎰", "🎲", "🧩", "🧸", "🪅", "🪩", "🪆", "♠️",
                "♥️", "♦️", "♣️", "♟️", "🃏", "🎴", "🎭", "🖼️", "🎨",
                "🧵", "🪡", "🧶", "🪢", "👓", "🕶️", "🥽", "🥼", "🦺",
                "👔", "👕", "👖", "🧣", "🧤", "🧥", "🧦", "👗", "👘",
                "🥻", "🩱", "🩲", "🩳", "👙", "🩴", "👚", "👛", "👜",
                "👝", "🛍️", "🎒", "🩰", "👞", "👟", "🥾", "🥿", "👠",
                "👡", "🩴", "👢", "👑", "👒", "🎩", "🎓", "🧢", "🪖",
                "⛑️", "📿", "💄", "💍", "💎"
            ),

            // Objects
            "Objects" to listOf(
                "💌", "🕳️", "💣", "🪓", "🔪", "🗡️", "⚔️", "🛡️", "🚬",
                "⚰️", "🪦", "⚱️", "🏺", "🔮", "📿", "🧿", "🪬", "💈",
                "⚗️", "🔭", "🔬", "🕳️", "🩻", "💊", "💉", "🩸", "🩹",
                "🩺", "🩼", "🚪", "🛏️", "🛋️", "🪑", "🚽", "🪠", "🚿",
                "🛁", "🪞", "🪟", "🛒", "🛍️", "🎁", "🎀", "🪄", "📦",
                "📫", "📮", "🗳️", "✉️", "📩", "📤", "📥", "📦", "📜",
                "📃", "📑", "📊", "📈", "📉", "📄", "📰", "🗞️", "📖",
                "📚", "🔖", "🧷", "🔗", "📎", "🖇️", "📐", "📏", "🧮",
                "📌", "📍", "🖊️", "🖋️", "✒️", "🖌️", "🖍️", "📝", "✏️",
                "🔏", "🔐", "🔒", "🔓", "❤️‍🩹", "🩷", "🖤", "💔", "❤️‍🔥",
                "💘", "💝", "💖", "💗", "💓", "💞", "💕", "❣️", "💟",
                "🔔", "🔕", "🎵", "🎶", "🎼", "🎧", "📯", "🎷", "🪗",
                "🎸", "🎹", "🎺", "🎻", "🪕", "🥁", "🪘", "📱", "📲",
                "📞", "📟", "📠", "🔋", "🔌", "💻", "🖥️", "🖨️", "⌨️",
                "🖱️", "🖲️", "💽", "💾", "💿", "📀", "🧮", "🎥", "🎬",
                "📷", "📸", "📹", "📼", "🔍", "🔎", "🕯️", "💡", "🔦",
                "🏮", "🪔", "📔", "📒", "📕", "📗", "📘", "📙", "📓",
                "📔", "📒", "📜", "📄", "📑", "🔖", "🏷️", "💰", "🪙",
                "💴", "💵", "💶", "💷", "💸", "💳", "🧾", "💹", "📥",
                "📤", "📦", "📤", "📥", "📪", "📫", "📬", "📭", "📮",
                "📜", "📃", "📑", "📊", "📈", "📉", "🗃️", "🗄️", "🗑️",
                "🔒", "🔓", "🔏", "🔐", "🔑", "🗝️", "🔨", "🪓", "⛏️",
                "⚒️", "🛠️", "🗡️", "⚔️", "🔧", "🔩", "🪛", "🔗", "🪝",
                "🧰", "🧲", "🪜", "⚙️", "🛞", "💈", "⚗️", "🪆", "🧪",
                "🧫", "🧬", "🔬", "🔭", "📡", "💣", "🪖", "🔫", "🪃",
                "🪚", "🪃", "🛡️", "🔧", "🪛", "🪜", "🛏️", "🛋️", "🪑",
                "🚪", "🪞", "🛗", "🪟", "🛠️", "🗝️", "🪒", "🪦", "⚰️",
                "⚱️", "🪞", "🔪", "🧴", "🧷", "🧸", "🪆", "🪅", "🪇",
                "🪖", "🪩", "📯", "📡", "🔨", "📢", "📣", "📖", "🔗",
                "🧲", "🧮"
            ),

            // Symbols
            "Symbols" to listOf(
                "❤️", "🩷", "🧡", "💛", "💚", "💙", "🩵", "💜", "🩶",
                "🖤", "🤍", "🤎", "💔", "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞",
                "💓", "💗", "💖", "💘", "💝", "🔞", "📴", "📳", "🈶",
                "🈚", "🈸", "🈺", "🈷️", "✴️", "🆚", "💮", "🉐", "㊗️",
                "㊙️", "🈴", "🈵", "🈹", "🈲", "🅰️", "🅱️", "🆎", "🆑",
                "🅾️", "🆘", "❌", "⭕", "🛑", "⛔", "📛", "🚫", "💯",
                "💢", "♨️", "🚷", "🚯", "🚳", "🚱", "🔞", "📵", "🚭",
                "❗", "❕", "❓", "❔", "‼️", "⁉️", "🔅", "🔆", "〽️",
                "⚠️", "🚸", "🔱", "⚜️", "🔰", "♻️", "✅", "🈯", "💹",
                "❇️", "✳️", "❎", "🌐", "💠", "Ⓜ️", "🌀", "💤", "🏧",
                "🚾", "♿", "🅿️", "🛗", "🈳", "🈂️", "🛂", "🛃", "🛄",
                "🛅", "🚹", "🚺", "🚼", "⚧️", "🚻", "🚮", "🎦", "📶",
                "🈁", "🔣", "ℹ️", "🔤", "🔡", "🔠", "🆖", "🆗", "🆙",
                "🆒", "🆕", "🆓", "0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣",
                "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟", "🔢", "#️⃣", "*️⃣", "⏏️",
                "▶️", "⏸️", "⏯️", "⏹️", "⏺️", "⏭️", "⏮️", "⏩", "⏪",
                "⏫", "⏬", "◀️", "🔼", "🔽", "➡️", "⬅️", "⬆️", "⬇️",
                "↔️", "↕️", "↖️", "↗️", "↘️", "↙️", "↩️", "↪️", "⤴️",
                "⤵️", "🔀", "🔁", "🔂", "🔄", "🔃", "🎵", "🎶", "➕",
                "➖", "➗", "✖️", "♾️", "💲", "💱", "™️", "©️", "®️",
                "〰️", "➰", "➿", "🔚", "🔙", "🔛", "🔝", "🔜", "✔️",
                "☑️", "🔘", "⚪", "⚫", "🔴", "🔵", "🔺", "🔻", "🔸",
                "🔹", "🔶", "🔷", "🔳", "🔲", "▪️", "▫️", "◼️", "◻️",
                "◾", "◽", "🟥", "🟧", "🟨", "🟩", "🟦", "🟪", "🟫", "⬛",
                "⬜", "🔈", "🔇", "🔉", "🔊", "🔔", "🔕", "📣", "📢",
                "👁️‍🗨️", "💬", "💭", "🗯️", "♠️", "♣️", "♥️", "♦️", "🃏",
                "🎴", "🀄", "🕐", "🕑", "🕒", "🕓", "🕔", "🕕", "🕖",
                "🕗", "🕘", "🕙", "🕚", "🕛", "🕜", "🕝", "🕞", "🕟",
                "🕠", "🕡", "🕢", "🕣", "🕤", "🕥", "🕦", "🕧", "✉️",
                "📧", "📨", "📩", "📤", "📥", "📦", "📫", "📪", "📬",
                "📭", "📮", "🗳️", "✏️", "✒️", "🖋️", "🖊️", "🖌️", "🖍️",
                "📝", "💼", "📁", "📂", "🗂️", "📅", "📆", "🗒️", "🗓️",
                "📇", "📈", "📉", "📊", "📋", "📌", "📍", "📎", "🖇️",
                "📏", "📐", "✂️", "🗃️", "🗄️", "🗑️", "🔒", "🔓", "🔏",
                "🔐", "🔑", "🗝️"
            ),

            // Flags
            "Flags" to listOf(
                "🏁", "🚩", "🎌", "🏴", "🏳️", "🏳️‍🌈", "🏳️‍⚧️", "🏴‍☠️", "🇦🇨",
                "🇦🇩", "🇦🇪", "🇦🇫", "🇦🇬", "🇦🇮", "🇦🇱", "🇦🇲", "🇦🇴", "🇦🇶", "🇦🇷",
                "🇦🇸", "🇦🇹", "🇦🇺", "🇦🇼", "🇦🇽", "🇦🇿", "🇧🇦", "🇧🇧", "🇧🇩", "🇧🇪",
                "🇧🇫", "🇧🇬", "🇧🇭", "🇧🇮", "🇧🇯", "🇧🇱", "🇧🇲", "🇧🇳", "🇧🇴", "🇧🇶",
                "🇧🇷", "🇧🇸", "🇧🇹", "🇧🇻", "🇧🇼", "🇧🇾", "🇧🇿", "🇨🇦", "🇨🇨", "🇨🇩",
                "🇨🇫", "🇨🇬", "🇨🇭", "🇨🇮", "🇨🇰", "🇨🇱", "🇨🇲", "🇨🇳", "🇨🇴", "🇨🇵",
                "🇨🇷", "🇨🇺", "🇨🇻", "🇨🇼", "🇨🇽", "🇨🇾", "🇨🇿", "🇩🇪", "🇩🇬", "🇩🇯",
                "🇩🇰", "🇩🇲", "🇩🇴", "🇩🇿", "🇪🇦", "🇪🇨", "🇪🇪", "🇪🇬", "🇪🇭", "🇪🇷",
                "🇪🇸", "🇪🇹", "🇪🇺", "🇫🇮", "🇫🇯", "🇫🇰", "🇫🇲", "🇫🇴", "🇫🇷", "🇬🇦",
                "🇬🇧", "🇬🇩", "🇬🇪", "🇬🇫", "🇬🇬", "🇬🇭", "🇬🇮", "🇬🇱", "🇬🇲", "🇬🇳",
                "🇬🇵", "🇬🇶", "🇬🇷", "🇬🇸", "🇬🇹", "🇬🇺", "🇬🇼", "🇬🇾", "🇭🇰", "🇭🇲",
                "🇭🇳", "🇭🇷", "🇭🇹", "🇭🇺", "🇮🇨", "🇮🇩", "🇮🇪", "🇮🇱", "🇮🇲", "🇮🇳",
                "🇮🇴", "🇮🇶", "🇮🇷", "🇮🇸", "🇮🇹", "🇯🇪", "🇯🇲", "🇯🇴", "🇯🇵", "🇰🇪",
                "🇰🇬", "🇰🇭", "🇰🇮", "🇰🇲", "🇰🇳", "🇰🇵", "🇰🇷", "🇰🇼", "🇰🇾", "🇰🇿",
                "🇱🇦", "🇱🇧", "🇱🇨", "🇱🇮", "🇱🇰", "🇱🇷", "🇱🇸", "🇱🇹", "🇱🇺", "🇱🇻",
                "🇱🇾", "🇲🇦", "🇲🇨", "🇲🇩", "🇲🇪", "🇲🇫", "🇲🇬", "🇲🇭", "🇲🇰",
                "🇲🇱", "🇲🇲", "🇲🇳", "🇲🇴", "🇲🇵", "🇲🇶", "🇲🇷", "🇲🇸", "🇲🇹",
                "🇲🇺", "🇲🇻", "🇲🇼", "🇲🇽", "🇲🇾", "🇲🇿", "🇳🇦", "🇳🇨", "🇳🇪", "🇳🇫",
                "🇳🇬", "🇳🇮", "🇳🇱", "🇳🇴", "🇳🇵", "🇳🇷", "🇳🇺", "🇳🇿", "🇴🇲", "🇵🇦",
                "🇵🇪", "🇵🇫", "🇵🇬", "🇵🇭", "🇵🇰", "🇵🇱", "🇵🇲", "🇵🇳", "🇵🇷", "🇵🇸",
                "🇵🇹", "🇵🇼", "🇵🇾", "🇶🇦", "🇷🇪", "🇷🇴", "🇷🇸", "🇷🇺", "🇷🇼", "🇸🇦",
                "🇸🇧", "🇸🇨", "🇸🇩", "🇸🇪", "🇸🇬", "🇸🇭", "🇸🇮", "🇸🇯", "🇸🇰", "🇸🇱",
                "🇸🇲", "🇸🇳", "🇸🇴", "🇸🇷", "🇸🇸", "🇸🇹", "🇸🇻", "🇸🇽", "🇸🇾", "🇸🇿",
                "🇹🇦", "🇹🇨", "🇹🇩", "🇹🇫", "🇹🇬", "🇹🇭", "🇹🇯", "🇹🇰", "🇹🇱", "🇹🇲", "🇹🇳",
                "🇹🇴", "🇹🇷", "🇹🇹", "🇹🇻", "🇹🇼", "🇹🇿", "🇺🇦", "🇺🇬", "🇺🇲", "🇺🇸", "🇺🇾",
                "🇺🇿", "🇻🇦", "🇻🇨", "🇻🇪", "🇻🇬", "🇻🇮", "🇻🇳", "🇻🇺", "🇼🇫", "🇼🇸",
                "🇽🇰", "🇾🇪", "🇾🇹", "🇿🇦", "🇿🇲", "🇿🇼", "🏴‍☠️"
            ),
        )

        fun addToRecent(emoji: String) {
            val recentList = emojisByRecentCategory["Recent"] ?: mutableListOf()
            if (recentList.contains(emoji)) {
                // Remove the emoji if it's already in the list (to bring it to the front)
                recentList.remove(emoji)
            }
            recentList.add(0, emoji) // Add the emoji at the beginning
            if (recentList.size > 30) {
                recentList.removeAt(recentList.size - 1) // Keep only the last 30 emojis
            }
            emojisByRecentCategory["Recent"] = recentList
        }
        fun populateRecentEmojis() {
            emojiBinding.emojiContainer.removeAllViews()  // Clear previous emojis

            val recentEmojis = emojisByRecentCategory["Recent"] ?: return

            // If there are no recent emojis, show a "No Recent Emoji Found" message
            if (recentEmojis.isEmpty()) {
                val noRecentEmojiText = TextView(this).apply {
                    text = context.getString(R.string.no_recent_emoji_found)
                    textSize = 20f
                    setPadding(16, 16, 16, 16)
                    gravity = Gravity.CENTER
                }
                emojiBinding.emojiContainer.addView(noRecentEmojiText)
                return
            }

            val rowEmojis = mutableListOf<String>()

            recentEmojis.forEachIndexed { index, emoji ->
                rowEmojis.add(emoji)

                // When the row has 9 emojis or we reach the last emoji
                if (rowEmojis.size == 9 || index == recentEmojis.lastIndex) {
                    val rowLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setPadding(0, 8, 0, 8)
                    }

                    rowEmojis.forEach { rowEmoji ->
                        val emojiView = TextView(this).apply {
                            text = rowEmoji
                            textSize = 25f
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(4, 8, 4, 8)
                            }
                            setPadding(8, 8, 8, 8)
                            setOnClickListener {
                                currentInputConnection?.commitText("$rowEmoji ", 1)
                                addToRecent(rowEmoji)  // Add to recent when clicked
                            }
                        }
                        rowLayout.addView(emojiView)
                    }

                    emojiBinding.emojiContainer.addView(rowLayout)
                    rowEmojis.clear()  // Clear after each row
                }
            }
        }

        fun populateEmojis(category: String) {
            emojiBinding.emojiContainer.removeAllViews()

            val emojis = emojisByCategory[category] ?: return
            val rowEmojis = mutableListOf<String>()
            emojis.forEachIndexed { index, emoji ->
                rowEmojis.add(emoji)

                if (rowEmojis.size == 9 || index == emojis.lastIndex) {
                    val rowLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setPadding(0, 8, 0, 8)
                    }

                    rowEmojis.forEach { rowEmoji ->
                        val emojiView = TextView(this).apply {
                            text = rowEmoji
                            textSize = 25f
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(4, 8, 4, 8)
                            }
                            setPadding(8, 8, 8, 8)
                            setOnClickListener {
                                currentInputConnection?.commitText("$rowEmoji ", 1)
                                addToRecent(rowEmoji)
                            }
                        }
                        rowLayout.addView(emojiView)
                    }

                    emojiBinding.emojiContainer.addView(rowLayout)
                    rowEmojis.clear()
                }
            }
        }
        emojiBinding.btnBackspace.setOnTouchListener { _, event ->
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
        // Bind category buttons
        emojiBinding.btnSmileys.setOnClickListener {
            populateEmojis("Smileys")
        }
        emojiBinding.btnRecent.setOnClickListener {
            populateRecentEmojis()  // Show recent emojis when the button is clicked
        }
        emojiBinding.btnAnimals.setOnClickListener {
            populateEmojis("Animals")
        }
        emojiBinding.btnFood.setOnClickListener {
            populateEmojis("Food")
        }
        emojiBinding.btnActivity.setOnClickListener {
            populateEmojis("Activities")
        }
        emojiBinding.btnObjects.setOnClickListener {
            populateEmojis("Objects")
        }
        emojiBinding.btnSymbols.setOnClickListener {
            populateEmojis("Symbols")
        }
        emojiBinding.btnFlags.setOnClickListener {
            populateEmojis("Flags")
        }

        // Populate default category (Recent)
        populateRecentEmojis()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        Log.i(TAG, "onUpdateSelection: called")

        val inputText = currentInputConnection?.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString() ?: ""
        val autocompleteArea = activeView?.findViewById<LinearLayout>(R.id.autocompleteArea)
        val suggestion1 = activeView?.findViewById<TextView>(R.id.suggestion1)
        val suggestion2 = activeView?.findViewById<TextView>(R.id.suggestion2)
        val suggestion3 = activeView?.findViewById<TextView>(R.id.suggestion3)
        val clipboardScroll = normalBinding.clipboardScroll
        val clipboardContainer = normalBinding.clipboardContainer
        Log.i(TAG, "values --> $inputText, $suggestion3, $suggestion2, $suggestion1 ")

        if (autocompleteArea != null && suggestion1 != null && suggestion2 != null && suggestion3 != null) {
            if (inputText.isNotEmpty()) {
                // Show clipboard content
                clipboardContainer.visibility = View.GONE
                clipboardScroll.visibility = View.GONE
                // Configure autocomplete logic (once)
                configureAutocomplete(autocompleteArea, listOf(suggestion1, suggestion2, suggestion3))

                // Update suggestions dynamically
                updateAutocompleteSuggestions(inputText, autocompleteArea, listOf(suggestion1, suggestion2, suggestion3), this)

                // Set click listeners for suggestions
                val suggestions = listOf(suggestion1, suggestion2, suggestion3)
                for (suggestion in suggestions) {
                    suggestion.setOnClickListener {
                        val selectedSuggestion = suggestion.text.toString()
                        if (selectedSuggestion.isNotEmpty()) {
                            // Get the last word or segment of the input text
                            val lastInputText = inputText.split(" ").lastOrNull() ?: ""
                            // Delete the existing input text
                            currentInputConnection?.deleteSurroundingText(lastInputText.length, 0)
                            // Insert the suggestion and add a space
                            currentInputConnection?.commitText("$selectedSuggestion ", 1)
                            // Hide the autocomplete area
                            autocompleteArea.visibility = View.GONE
                        }
                    }
                }
            } else {
                autocompleteArea.visibility = View.GONE
                // Show clipboard content
                clipboardContainer.visibility = View.VISIBLE
                clipboardScroll.visibility = View.VISIBLE
            }
        }
    }


    private fun configureAutocomplete(autocompleteArea: LinearLayout, suggestions: List<TextView>) {
        // Setup listeners and visibility logic
        autocompleteArea.visibility = View.GONE // Start with hidden area
        suggestions.forEach { suggestion ->
            suggestion.visibility = View.GONE
            suggestion.setOnClickListener {
                currentInputConnection?.commitText(suggestion.text, 1)
            }
        }
    }

    private fun updateAutocompleteSuggestions(
        inputText: String,
        autocompleteArea: LinearLayout,
        suggestions: List<TextView>,
        context: Context
    ) {
        // Get the last word typed by the user
        val lastInputText = inputText.split(" ").lastOrNull()?.lowercase() ?: ""
        Log.i(TAG, "updateAutocompleteSuggestions: Last input text: $lastInputText")
        saveUserInput(this@MyKeyboard, lastInputText)
        if (lastInputText.isNotEmpty()) {
            // Step 1: Check if the word exists in the user dictionary
            val userDict = getUserDictionary(context) // Retrieve user dictionary
            val userDictSuggestions = userDict.filter { it.startsWith(lastInputText, ignoreCase = true) }
            Log.i(TAG, "updateAutocompleteSuggestions: userDictSuggestions  $userDictSuggestions",)
            // Step 2: Fill up to 2 suggestions from the user dictionary
            val suggestionsToShow = mutableListOf<String>()

            // Add up to 2 suggestions from the user dictionary
            suggestionsToShow.addAll(userDictSuggestions.take(2))

            // Step 3: If there are fewer than 3 suggestions, fill the remaining slots from the Trie
            if (suggestionsToShow.size < 3) {
                val remainingSuggestions = trie.searchByPrefix(lastInputText)
                // Add suggestions from the Trie that are not already in the user dictionary
                remainingSuggestions.forEach { suggestion ->
                    // Ensure no duplicates: only add if it's not already in userDictSuggestions
                    if (!suggestionsToShow.contains(suggestion)) {
                        suggestionsToShow.add(suggestion)
                    }
                }
            }

            // Step 4: Update the suggestion views
            for (i in suggestions.indices) {
                if (i < suggestionsToShow.size) {
                    suggestions[i].text = suggestionsToShow[i]
                    suggestions[i].visibility = View.VISIBLE
                } else {
                    suggestions[i].visibility = View.GONE
                }
            }

            // Step 5: Show or hide the autocomplete area
            autocompleteArea.visibility = if (suggestionsToShow.isEmpty()) View.GONE else View.VISIBLE
        } else {
            autocompleteArea.visibility = View.GONE
        }
    }

    // Retrieve the user dictionary from SharedPreferences
    private fun getUserDictionary(context: Context): Set<String> {
        val sharedPref = context.getSharedPreferences("UserDict", Context.MODE_PRIVATE)
        return sharedPref.getStringSet("user_dict", mutableSetOf()) ?: mutableSetOf()
    }

    // Save new user input to the dictionary, avoiding duplicates
    private fun saveUserInput(context: Context, input: String) {
        val sharedPref = context.getSharedPreferences("UserDict", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        // Retrieve the existing dictionary or create a new one if it doesn't exist
        val existingDict = sharedPref.getStringSet("user_dict", mutableSetOf()) ?: mutableSetOf()
        Log.i(TAG, "saveUserInput: existingDict")
        // Check if the word is already in the dictionary, and add it only if it's not present
        if (!existingDict.contains(input)) {
            Log.i(TAG, "saveUserInput: $input")
            existingDict.add(input)  // Add the new word
            editor.putStringSet("user_dict", existingDict) // Save the updated dictionary
            editor.apply()  // Asynchronously save
        }
    }



    private fun setupClipboardLogic(
        clipboardScroll: HorizontalScrollView,
        clipboardContainer: LinearLayout
    ) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        Log.i(TAG, "Clipboard Manager initialized")

        // Listener to update clipboard content in real-time
        clipboardManager.addPrimaryClipChangedListener {
            Log.i(TAG, "Clipboard content changed")
            updateClipboardContent(clipboardManager, clipboardContainer, clipboardScroll)
        }

        // Initial setup for clipboard content
        updateClipboardContent(clipboardManager, clipboardContainer, clipboardScroll)
    }

    private fun updateClipboardContent(
        clipboardManager: ClipboardManager,
        clipboardContainer: LinearLayout,
        clipboardScroll: HorizontalScrollView
    ) {
        // Clear existing clipboard content in the UI
        clipboardContainer.removeAllViews()

        val clipboardData = clipboardManager.primaryClip
        Log.i(TAG, "Clipboard data: $clipboardData")

        if (clipboardData != null && clipboardData.itemCount > 0) {
            clipboardScroll.visibility = View.VISIBLE

            for (i in 0 until clipboardData.itemCount) {
                val text = clipboardData.getItemAt(i).text
                Log.i(TAG, "Clipboard text: $text")

                if (!text.isNullOrEmpty()) {
                    val textView = TextView(this).apply {
                        this.text = text
                        textSize = 16f
                        setPadding(16, 16, 16, 16)
                        setOnClickListener {
                            currentInputConnection?.commitText(this.text, 1)
                        }
                    }
                    Log.i(TAG, "Clipboard: adding to textview")
                    clipboardContainer.addView(textView)
                }
            }
        } else {
            clipboardScroll.visibility = View.GONE // Hide the clipboard UI if there's no data
        }
    }


    private fun setupAutocompleteLogic(autocompleteArea: LinearLayout, suggestions: List<TextView>) {
        fun showSuggestions(inputText: String) {
            updateAutocompleteSuggestions(inputText, autocompleteArea, suggestions, this)
        }
    }

    /**
     * Configures the keyboard layout and its button listeners.
     */
    private fun setupKeyboardLayout(binding: KeyboardLayoutBinding) {
        val clipboardScroll = binding.clipboardScroll
        val clipboardContainer = binding.clipboardContainer
        val autocompleteArea = binding.autocompleteArea
        val suggestion1 = binding.suggestion1
        val suggestion2 = binding.suggestion2
        val suggestion3 = binding.suggestion3



        // Initialize Clipboard Logic
        setupClipboardLogic(clipboardScroll, clipboardContainer)

        // Initialize Autocomplete Logic
        setupAutocompleteLogic(autocompleteArea, listOf(suggestion1, suggestion2, suggestion3))
        Log.i(TAG, "setupKeyboardLayout: Configuring normal keyboard layout")
        setupButtonListeners(getNumericButtons()) // Setup numeric buttons
        setupButtonListeners(getAlphabeticButtons()) // Setup alphabetic buttons
        setupBackspaceButton(binding) // Setup backspace button
        setupActionButtons(binding.root) // Setup action buttons like Enter, Space, etc.
        setupToggleSpecialKeysButton(binding) // Setup the toggle button to switch layouts like normal and special keys
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
     * Switches the keyboard layout dynamically based on the isSpecialKeysEnabled flag.
     */
    private fun switchKeyboardLayout(button: Button) {
        Log.i(TAG, "switchKeyboardLayout: Switching layout to ${if (isSpecialKeysEnabled) "special" else "normal"} keyboard")

        activeView = if (isSpecialKeysEnabled) {
            setupKeyboardLayoutForSpecial(specialBinding)
            specialBinding.functionalKeys.btnSpecialKeys.text="abc"
            specialBinding.root
        } else{
            setupKeyboardLayout(normalBinding)
            normalBinding.functionalKeys.btnSpecialKeys.text="!?"
            normalBinding.root
        }

        // Update the input view
        setInputView(activeView)
    }
    /**
     * Configures the special keys layout.
     */
    private fun setupKeyboardLayoutForSpecial(specialBinding: KeyboardSpecialBinding) {
        Log.i(TAG, "setupKeyboardLayoutForSpecial: Configuring special keyboard layout")
        // Array of buttons and their corresponding characters
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
class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    var isEndOfWord = false
}


fun loadDictionaryFromAssets(context: Context): List<String> {
    val wordList = mutableListOf<String>()

    try {
        // Open the ZIP file from the assets folder
        context.assets.open("dictionary.zip").use { inputStream ->
            ZipInputStream(inputStream).use { zipStream ->
                var entry = zipStream.nextEntry
                Log.i("DictionaryLoader", " to load dictionary: $entry")
                while (entry != null) { // Iterate through all entries in the ZIP file
                    if (!entry.isDirectory) {
                        BufferedReader(InputStreamReader(zipStream)).use { reader ->
                            reader.lineSequence().forEach { line ->
                                wordList.add(line.trim())
                            }
                        }
                    }
                    zipStream.closeEntry() // Close the current entry
                    entry = zipStream.nextEntry // Move to the next entry
                }
            }
        }
    } catch (e: Exception) {
        Log.e("DictionaryLoader", "Failed to load dictionary: ${e.message}", e)
    }

    return wordList
}

class Trie {
    private val root = TrieNode()

    // Insert a word into the Trie
    fun insert(word: String) {
        var node = root
        for (char in word) {
            node = node.children.getOrPut(char) { TrieNode() }
        }
        node.isEndOfWord = true
    }

    // Search for all words matching a prefix
    fun searchByPrefix(prefix: String): List<String> {
        val result = mutableListOf<String>()
        var node = root

        // Navigate to the end of the prefix
        for (char in prefix) {
            node = node.children[char] ?: return result // Prefix not found
        }

        // Perform DFS to find all words with the given prefix
        dfs(node, prefix, result)
        return result
    }

    private fun dfs(node: TrieNode, prefix: String, result: MutableList<String>) {
        if (node.isEndOfWord) {
            result.add(prefix)
        }
        for ((char, child) in node.children) {
            dfs(child, prefix + char, result)
        }
    }
}
