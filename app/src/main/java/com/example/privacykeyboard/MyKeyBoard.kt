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
import com.example.privacykeyboard.databinding.KeyboardLayoutBinding
import com.example.privacykeyboard.databinding.KeyboardSpecialBinding
// Core Android components
import android.content.Context
// For clipboard functionality
import android.content.ClipboardManager
import android.text.TextUtils
import android.view.Gravity
import android.view.inputmethod.ExtractedTextRequest

// For UI components like GridLayout and HorizontalScrollView
import android.widget.HorizontalScrollView
import android.widget.LinearLayout


// For Input Connection updates
import androidx.core.content.ContextCompat
import com.example.privacykeyboard.databinding.EmojiLayoutBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream


class MyKeyboard : InputMethodService() {
    // Constant for logging, renamed for clarity and following convention
    private val TAG: String = "PrivacyKeyboard"

    // Boolean flag for Caps Lock state, use `val` if it's not changing after initialization
    private var isCapsLockActive: Boolean = true

    // Binding variables initialized using lateinit to reference the layouts
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

    // Constants for recent emojis handling
    private val RECENT_EMOJIS_KEY = "recent_emojis" // Key to store recent emojis in preferences
    private val MAX_RECENT_EMOJIS = 50 // Max number of recent emojis to store

    override fun onCreateInputView(): View {
        // Inflate keyboard layouts using View Binding
        normalBinding = KeyboardLayoutBinding.inflate(layoutInflater) // Inflate the normal keyboard layout
        specialBinding = KeyboardSpecialBinding.inflate(layoutInflater) // Inflate the special keyboard layout
        emojiBinding = EmojiLayoutBinding.inflate(layoutInflater) // Inflate the emoji keyboard layout

        // Add emoji layout to the normal layout container
        normalBinding.normalContainer.addView(emojiBinding.root) // Add emoji layout root to the main keyboard container

        // Initialize keyboard rows container and set initial visibility
        keyboardRowsContainer = normalBinding.keyboardRowsContainer // Get reference to the keyboard rows container
        keyboardRowsContainer.visibility = View.VISIBLE // Make sure keyboard rows container is visible

        // Initially hide the emoji layout
        emojiBinding.root.visibility = View.GONE // Hide the emoji keyboard initially

        // Set initial layout to normal keyboard view
        activeView = normalBinding.root // Set the normal keyboard layout as the active view
        setupKeyboardLayout(normalBinding) // Initialize and set up the normal keyboard layout

        // Configure emoji button click listener to toggle emoji layout visibility
        normalBinding.btnEmoji.setOnClickListener { toggleEmojiLayout() } // Set a click listener for the emoji button to toggle the emoji layout

        // Set the height of the emoji keyboard ScrollView dynamically
        setEmojiKeyboardHeight() // Adjust the emoji keyboard scroll view height based on available screen space

        // Initialize the Trie with dictionary data (for word prediction or suggestions)
        initializeTrie(this) // Set up the Trie data structure with necessary dictionary

        // Return the current active view (the normal keyboard layout)
        return activeView!! // Return the root view for the normal keyboard layout, ensuring it’s not null
    }


    /**
     * Sets the height of the emoji keyboard ScrollView dynamically based on screen height.
     */
    private fun setEmojiKeyboardHeight() {
        // Get the display metrics to retrieve screen dimensions
        val displayMetrics = resources.displayMetrics // Get the device's display metrics (screen info like height, width, density, etc.)

        // Retrieve the screen height in pixels
        val screenHeight = displayMetrics.heightPixels // Get the height of the screen in pixels

        // Set keyboard height to approximately 30% of screen height (adjust factor if needed)
        val keyboardHeight = (screenHeight * 0.3).toInt() // Calculate the desired keyboard height as 30% of the screen height, rounded to an integer

        // Update the height of the emoji keyboard's ScrollView to the calculated value
        emojiBinding.scrollView.layoutParams.height = keyboardHeight // Set the new height for the ScrollView container that holds the emoji keyboard
    }


    /**
     * Called when the input method service is destroyed.
     * Unregisters the clipboard listener to prevent memory leaks.
     */
    override fun onDestroy() {
        // Call the superclass method to ensure proper cleanup
        super.onDestroy()

        // Get the ClipboardManager system service to interact with the clipboard
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Unregister the listener from the ClipboardManager
        clipboardManager.removePrimaryClipChangedListener { } // Remove any previously registered listeners from clipboard changes
    }


    /**
     * Initializes the Trie data structure with words from the dictionary.
     * @param context The application context, used to load the dictionary file.
     */
    private fun initializeTrie(context: Context) {
        // Create a new Trie instance to store and search words efficiently
        trie = Trie()

        // Load the dictionary of words from the assets folder
        val wordList = loadDictionaryFromAssets(context)

        // Insert each word from the dictionary into the Trie for efficient searching
        wordList.forEach { word ->
            trie.insert(word)
        }

    }

    /**
     * Makes the keyboard rows container visible, allowing the user to see the keyboard layout.
     */
    private fun showKeyboardRows() {
        // Set the visibility of the keyboard rows container to VISIBLE
        keyboardRowsContainer.visibility = View.VISIBLE
    }

    /**
     * Hides the keyboard rows container, making the keyboard rows invisible.
     */
    private fun hideKeyboardRows() {
        // Set the visibility of the keyboard rows container to GONE, which will hide it from the screen
        keyboardRowsContainer.visibility = View.GONE
    }

    /**
     * Toggles the visibility of the emoji layout and keyboard rows.
     * If the emoji layout is visible, it will be hidden and the keyboard rows will be shown.
     * If the emoji layout is not visible, it will be shown and the keyboard rows will be hidden.
     */
    private fun toggleEmojiLayout() {
        // Check if the emoji layout is currently visible
        if (emojiBinding.root.visibility == View.VISIBLE) {
            // If emoji layout is visible, hide it
            emojiBinding.root.visibility = View.GONE
            // Show the keyboard rows again
            showKeyboardRows()
        } else {
            // If emoji layout is not visible, hide the keyboard rows
            hideKeyboardRows()
            // Show the emoji layout
            emojiBinding.root.visibility = View.VISIBLE
            // Set up the emoji picker layout or data
            setupEmojiPicker()
        }
    }


    private fun setupEmojiPicker() {
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


        /**
         * Populates the emoji container with emojis based on the selected category.
         * This function organizes emojis into rows of 7 emojis per row and adds them to the layout.
         *
         * @param category The category of emojis to display (e.g., smileys, animals, etc.).
         */
        fun populateEmojis(category: String) {
            // Remove all existing views from the emoji container to prepare for new content
            emojiBinding.emojiContainer.removeAllViews()

            // Get the list of emojis for the selected category. If no emojis are found, exit early.
            val emojis = emojisByCategory[category] ?: return

            // Temporary list to store emojis for the current row
            val rowEmojis = mutableListOf<String>()

            // Loop through the emojis in the selected category
            emojis.forEachIndexed { index, emoji ->
                // Add each emoji to the rowEmojis list
                rowEmojis.add(emoji)

                // If the row has 7 emojis or if it's the last emoji in the list, create a new row
                if (rowEmojis.size == 7 || index == emojis.lastIndex) {
                    // Create a new LinearLayout for the row with horizontal orientation
                    val rowLayout = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setPadding(0, 8, 0, 8) // Add some padding to the row
                    }

                    // For each emoji in the row, create a TextView to display it
                    rowEmojis.forEach { rowEmoji ->
                        val emojiView = TextView(this).apply {
                            text = rowEmoji // Set the emoji as text
                            textSize = 39f // Set the size of the emoji
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(4, 8, 4, 8) // Add margins to space out the emojis
                            }
                            setPadding(8, 8, 8, 8) // Add padding inside each emoji view

                            // Set a ripple effect for the button click interaction
                            background = ContextCompat.getDrawable(context, R.drawable.ripple_effect)
                            isClickable = true // Make the view clickable
                            isFocusable = true // Make the view focusable

                            // Set the action for when an emoji is clicked
                            setOnClickListener {
                                // Commit the emoji to the input connection (inserting the emoji in the text field)
                                currentInputConnection?.commitText("$rowEmoji ", 1)

                                // Add the emoji to the recent emojis list
                                addToRecent(rowEmoji)
                            }
                        }

                        // Add the emoji view to the row layout
                        rowLayout.addView(emojiView)
                    }

                    // Add the completed row layout to the emoji container
                    emojiBinding.emojiContainer.addView(rowLayout)

                    // Clear the rowEmojis list to start a new row for the next batch of emojis
                    rowEmojis.clear()
                }
            }
        }


        // Set an OnTouchListener for the backspace button to handle touch events
        emojiBinding.btnBackspace.setOnTouchListener { _, event ->
            when (event.action) {
                // When the button is pressed down, perform a single backspace and start continuous backspace
                MotionEvent.ACTION_DOWN -> {
                    isBackspacePressed = true // Mark backspace as pressed
                    performSingleBackspace() // Perform the single backspace action
                    startContinuousBackspace() // Start the continuous backspace if held down
                }
                // When the button is released or touch is canceled, stop the continuous backspace
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isBackspacePressed = false // Mark backspace as released
                    handler.removeCallbacksAndMessages(null) // Stop the continuous backspace actions
                }
            }
            true // Return true to indicate the event has been handled
        }

        // Bind category buttons to their respective actions
        emojiBinding.btnSmileys.setOnClickListener {
            populateEmojis("Smileys") // Populate the emoji grid with smiley emojis
        }
        emojiBinding.btnRecent.setOnClickListener {
            populateRecentEmojis()  // Populate the emoji grid with recent emojis
        }
        emojiBinding.btnAnimals.setOnClickListener {
            populateEmojis("Animals") // Populate the emoji grid with animal emojis
        }
        emojiBinding.btnFood.setOnClickListener {
            populateEmojis("Food") // Populate the emoji grid with food emojis
        }
        emojiBinding.btnActivity.setOnClickListener {
            populateEmojis("Activities") // Populate the emoji grid with activity-related emojis
        }
        emojiBinding.btnObjects.setOnClickListener {
            populateEmojis("Objects") // Populate the emoji grid with object-related emojis
        }
        emojiBinding.btnSymbols.setOnClickListener {
            populateEmojis("Symbols") // Populate the emoji grid with symbol-related emojis
        }
        emojiBinding.btnFlags.setOnClickListener {
            populateEmojis("Flags") // Populate the emoji grid with flag emojis
        }

        // Populate the emoji grid with the default category (Recent emojis)
        populateRecentEmojis()
    }

    /**
     * This method is called whenever the selection in the input field is updated.
     * It is responsible for updating the autocomplete suggestions based on the current input text,
     * and displaying or hiding the clipboard content if needed.
     *
     * If there is text in the input field, the method will:
     * - Hide the clipboard content.
     * - Display the autocomplete suggestions for the user to choose from.
     * - Allow the user to select a suggestion, which will replace the last word typed in the input field.
     *
     * If there is no input text, it will:
     * - Hide the autocomplete suggestions.
     * - Show the clipboard content (if available) for the user to paste from.
     *
     * @param oldSelStart The start of the previous selection (before the update).
     * @param oldSelEnd The end of the previous selection (before the update).
     * @param newSelStart The start of the new selection (after the update).
     * @param newSelEnd The end of the new selection (after the update).
     * @param candidatesStart The start position of the candidates for the input.
     * @param candidatesEnd The end position of the candidates for the input.
     */
    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        // Call the super method to maintain base behavior
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)

        // Get the current input text from the input connection (e.g., text in the input field)
        val inputText = currentInputConnection?.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString() ?: ""

        // Find the UI elements related to the autocomplete suggestions
        val autocompleteArea = activeView?.findViewById<LinearLayout>(R.id.autocompleteArea)
        val suggestion1 = activeView?.findViewById<TextView>(R.id.suggestion1)
        val suggestion2 = activeView?.findViewById<TextView>(R.id.suggestion2)
        val suggestion3 = activeView?.findViewById<TextView>(R.id.suggestion3)
        val clipboardScroll = normalBinding.clipboardScroll
        val clipboardContainer = normalBinding.clipboardContainer

        // Check if autocomplete views are available
        if (autocompleteArea != null && suggestion1 != null && suggestion2 != null && suggestion3 != null) {
            if (inputText.isNotEmpty()) {
                // Hide clipboard content when input text is available
                clipboardContainer.visibility = View.GONE
                clipboardScroll.visibility = View.GONE

                // Configure autocomplete area (e.g., position, styling)
                configureAutocomplete(autocompleteArea, listOf(suggestion1, suggestion2, suggestion3))

                // Update the autocomplete suggestions dynamically based on the current input text
                updateAutocompleteSuggestions(inputText, autocompleteArea, listOf(suggestion1, suggestion2, suggestion3), this)

                // Set click listeners for suggestion buttons
                val suggestions = listOf(suggestion1, suggestion2, suggestion3)
                for (suggestion in suggestions) {
                    suggestion.setOnClickListener {
                        val selectedSuggestion = suggestion.text.toString()
                        if (selectedSuggestion.isNotEmpty()) {
                            // Get the last word or segment of the input text
                            val lastInputText = inputText.split(" ").lastOrNull() ?: ""
                            // Delete the existing word from the input text
                            currentInputConnection?.deleteSurroundingText(lastInputText.length, 0)
                            // Insert the selected suggestion and add a space
                            currentInputConnection?.commitText("$selectedSuggestion ", 1)
                            // Hide the autocomplete area after selection
                            autocompleteArea.visibility = View.GONE
                        }
                    }
                }
            } else {
                // If input text is empty, hide the autocomplete area
                autocompleteArea.visibility = View.GONE
                // Show clipboard content (suggestions from clipboard)
                clipboardContainer.visibility = View.VISIBLE
                clipboardScroll.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Populates the emoji container with recent emojis stored in SharedPreferences.
     * If there are no recent emojis, a message indicating this will be shown.
     * The emojis are arranged in rows, with each row containing a maximum of 7 emojis.
     * When an emoji is clicked, it will be inserted into the current input connection and added to the recent emojis list.
     */
    private fun populateRecentEmojis() {
        // Clear any previously displayed emojis in the container
        emojiBinding.emojiContainer.removeAllViews()

        // Load recent emojis from SharedPreferences
        val recentEmojis = loadRecentEmojis()

        // If there are no recent emojis, display a message indicating that
        if (recentEmojis.isEmpty()) {
            val noRecentEmojiText = TextView(this).apply {
                text = context.getString(R.string.no_recent_emoji_found)
                textSize = 25f
                setPadding(16, 16, 16, 16)
                gravity = Gravity.CENTER
            }
            emojiBinding.emojiContainer.addView(noRecentEmojiText) // Add the "No Recent Emoji" message to the container
            return
        }

        // List to hold the emojis for each row
        val rowEmojis = mutableListOf<String>()

        // Loop through the list of recent emojis
        recentEmojis.forEachIndexed { index, emoji ->
            rowEmojis.add(emoji)

            // When the row contains 7 emojis or we reach the last emoji, create a new row
            if (rowEmojis.size == 7 || index == recentEmojis.lastIndex) {
                val rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(0, 8, 0, 8) // Add padding between rows
                }

                // Create a TextView for each emoji and add it to the current row layout
                rowEmojis.forEach { rowEmoji ->
                    val emojiView = TextView(this).apply {
                        text = rowEmoji
                        textSize = 39f
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(4, 8, 4, 8) // Set margin for each emoji
                        }
                        setPadding(8, 8, 8, 8) // Set padding around each emoji
                        // Add ripple effect when clicked
                        background = ContextCompat.getDrawable(context, R.drawable.ripple_effect)
                        isClickable = true
                        isFocusable = true

                        // When an emoji is clicked, commit it to the input connection and add to recent emojis
                        setOnClickListener {
                            currentInputConnection?.commitText("$rowEmoji ", 1)
                            addToRecent(rowEmoji) // Add emoji to recent emojis list
                        }
                    }
                    rowLayout.addView(emojiView) // Add the emoji to the row layout
                }

                // Add the completed row layout to the emoji container
                emojiBinding.emojiContainer.addView(rowLayout)
                rowEmojis.clear() // Clear the row list for the next set of emojis
            }
        }
    }

    /**
     * Adds the given emoji to the "recent emojis" list, ensuring that it is at the top of the list.
     * If the emoji already exists in the list, it will be removed and re-added at the top.
     * The size of the recent emojis list is capped at a maximum limit (defined by MAX_RECENT_EMOJIS).
     * The updated list is saved to SharedPreferences for persistence.
     *
     * @param emoji The emoji to be added to the recent emojis list.
     */
    private fun addToRecent(emoji: String) {
        // Load the current list of recent emojis from SharedPreferences and make it mutable
        val recentEmojis = loadRecentEmojis().toMutableList()

        // Remove the emoji if it already exists in the list to re-add it at the top
        if (recentEmojis.contains(emoji)) {
            recentEmojis.remove(emoji)
        }

        // Add the emoji to the top of the list (most recent)
        recentEmojis.add(0, emoji)

        // If the list exceeds the maximum allowed size, remove the oldest emoji
        if (recentEmojis.size > MAX_RECENT_EMOJIS) {
            recentEmojis.removeAt(recentEmojis.lastIndex)
        }

        // Save the updated list of recent emojis back to SharedPreferences
        saveRecentEmojis(recentEmojis)
    }

    /**
     * Loads the list of recent emojis from SharedPreferences.
     * If no recent emojis are found, an empty list is returned.
     * The emojis are stored as a comma-separated string in SharedPreferences.
     *
     * @return A list of recent emojis, or an empty list if no emojis are stored.
     */
    private fun loadRecentEmojis(): List<String> {
        // Get the SharedPreferences instance to access stored data
        val prefs = getSharedPreferences("emoji_prefs", Context.MODE_PRIVATE)

        // Retrieve the stored string of recent emojis, defaulting to an empty string if not found
        val emojiString = prefs.getString(RECENT_EMOJIS_KEY, "") ?: ""

        // If the emoji string is empty, return an empty list; otherwise, split the string into a list
        return if (emojiString.isEmpty()) {
            emptyList()
        } else {
            emojiString.split(",") // Split the string by commas to get the list of emojis
        }
    }

    /**
     * Saves the provided list of recent emojis to SharedPreferences.
     * The list is converted to a comma-separated string before being saved.
     *
     * @param emojis The list of emojis to be saved to SharedPreferences.
     */
    private fun saveRecentEmojis(emojis: List<String>) {
        // Get the SharedPreferences instance to store the updated data
        val prefs = getSharedPreferences("emoji_prefs", Context.MODE_PRIVATE)

        // Initialize SharedPreferences editor to make changes
        val editor = prefs.edit()

        // Convert the list of emojis to a comma-separated string
        val emojiString = emojis.joinToString(",")

        // Save the string of recent emojis in SharedPreferences
        editor.putString(RECENT_EMOJIS_KEY, emojiString)
        editor.apply() // Apply changes asynchronously
    }


    /**
     * Configures the autocomplete area and suggestion views by setting their initial visibility
     * and defining click actions for each suggestion.
     * The autocomplete area starts hidden, and suggestions are set to be hidden until needed.
     *
     * @param autocompleteArea The container layout for the autocomplete suggestions.
     * @param suggestions A list of TextView widgets representing the autocomplete suggestions.
     */
    private fun configureAutocomplete(autocompleteArea: LinearLayout, suggestions: List<TextView>) {
        // Initially hide the autocomplete area as it will be shown when there is input text
        autocompleteArea.visibility = View.GONE

        // Loop through each suggestion TextView to set initial visibility and click actions
        suggestions.forEach { suggestion ->
            // Initially hide each suggestion TextView
            suggestion.visibility = View.GONE

            // Set a click listener on each suggestion TextView
            suggestion.setOnClickListener {
                // When clicked, commit the suggestion text to the input connection (the text field)
                currentInputConnection?.commitText(suggestion.text, 1)
            }
        }
    }


    /**
     * Updates the autocomplete suggestions based on the last word typed by the user.
     * Suggestions are generated first from the user dictionary, and if there are fewer than 3 suggestions,
     * they are supplemented by additional suggestions from the Trie.
     *
     * @param inputText The current input text entered by the user.
     * @param autocompleteArea The LinearLayout container that holds the suggestion views.
     * @param suggestions A list of TextView views that display the autocomplete suggestions.
     * @param context The context used to retrieve the user dictionary.
     */
    private fun updateAutocompleteSuggestions(
        inputText: String,
        autocompleteArea: LinearLayout,
        suggestions: List<TextView>,
        context: Context
    ) {
        // Step 1: Get the last word typed by the user, ignoring case
        val lastInputText = inputText.split(" ").lastOrNull()?.lowercase() ?: ""

        // Only proceed if the last word typed is not empty
        if (lastInputText.isNotEmpty()) {
            // Step 2: Retrieve the user dictionary from SharedPreferences
            val userDict = getUserDictionary(context)

            // Filter the user dictionary to find words that start with the last input text
            val userDictSuggestions = userDict.filter { it.startsWith(lastInputText, ignoreCase = true) }

            // Prepare the list of suggestions to show, starting with suggestions from the user dictionary
            val suggestionsToShow = mutableListOf<String>()

            // Add up to 2 suggestions from the user dictionary
            suggestionsToShow.addAll(userDictSuggestions.take(2))

            // Step 3: If there are fewer than 3 suggestions, add more from the Trie
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

            // Step 4: Update the TextViews with the suggestions
            for (i in suggestions.indices) {
                if (i < suggestionsToShow.size) {
                    // Set the suggestion text and make it visible
                    suggestions[i].text = suggestionsToShow[i]
                    suggestions[i].visibility = View.VISIBLE
                } else {
                    // Hide the suggestion if there are fewer than 3
                    suggestions[i].visibility = View.GONE
                }
            }

            // Step 5: Show or hide the autocomplete area based on the number of suggestions
            autocompleteArea.visibility = if (suggestionsToShow.isEmpty()) View.GONE else View.VISIBLE
        } else {
            // Hide the autocomplete area if the input text is empty
            autocompleteArea.visibility = View.GONE
        }
    }


    /**
     * Retrieves the user-defined dictionary stored in SharedPreferences.
     * If no dictionary exists, returns an empty set.
     *
     * @param context The context used to access SharedPreferences.
     * @return A set containing the user-defined words.
     */
    private fun getUserDictionary(context: Context): Set<String> {
        // Retrieve the user dictionary from SharedPreferences
        val sharedPref = context.getSharedPreferences("UserDict", Context.MODE_PRIVATE)
        return sharedPref.getStringSet("user_dict", mutableSetOf()) ?: mutableSetOf()
    }

    /**
     * Saves a new user input (word) to the user dictionary in SharedPreferences.
     * It avoids saving duplicates and ensures the word is valid.
     *
     * @param context The context used to access SharedPreferences.
     * @param input The user input to be added to the dictionary.
     */
    private fun saveUserInput(context: Context, input: String) {
        // Get the SharedPreferences for storing the user dictionary
        val sharedPref = context.getSharedPreferences("UserDict", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        // Retrieve the existing dictionary from SharedPreferences, or create an empty set if not found
        val existingDict = sharedPref.getStringSet("user_dict", mutableSetOf()) ?: mutableSetOf()

        // Trim and validate the input before adding to the dictionary
        val trimmedInput = input.trim()

        if (isValidWord(trimmedInput) && !existingDict.contains(trimmedInput)) {
            Log.i(TAG, "saveUserInput: $trimmedInput")
            existingDict.add(trimmedInput)  // Add the new valid word to the dictionary
            editor.putStringSet("user_dict", existingDict)  // Save the updated dictionary
            editor.apply()  // Asynchronously save the changes to SharedPreferences
        } else {
            Log.i(TAG, "saveUserInput: Invalid or duplicate word - $trimmedInput")
        }
    }


    /**
     * Validates a word to ensure it meets certain criteria.
     * A valid word must be non-empty, have a length between 2 and 30 characters,
     * and consist only of alphabetic letters.
     *
     * @param word The word to be validated.
     * @return `true` if the word is valid, otherwise `false`.
     */
    private fun isValidWord(word: String): Boolean {
        // Regular expression to ensure the word contains only alphabetic characters
        val wordRegex = "^[a-zA-Z]+$".toRegex()  // Only allows alphabetic letters
        // Check if the word is not empty, has a length between 2 and 30, and matches the alphabetic pattern
        return word.isNotEmpty() && word.length in 2..30 && wordRegex.matches(word)
    }

    /**
     * Sets up the clipboard functionality by listening for changes in clipboard content.
     * This method also initializes the clipboard view to reflect the current clipboard content.
     *
     * @param clipboardScroll The horizontal scroll view containing clipboard items.
     * @param clipboardContainer The container holding clipboard content.
     */
    private fun setupClipboardLogic(
        clipboardScroll: HorizontalScrollView,
        clipboardContainer: LinearLayout
    ) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        Log.i(TAG, "Clipboard Manager initialized")

        // Listener for clipboard changes; it will update the clipboard view when content changes
        clipboardManager.addPrimaryClipChangedListener {
            Log.i(TAG, "Clipboard content changed")
            // Update the UI to reflect the new clipboard content
            updateClipboardContent(clipboardManager, clipboardContainer, clipboardScroll)
        }

        // Perform an initial update of the clipboard content to ensure the UI is up to date
        updateClipboardContent(clipboardManager, clipboardContainer, clipboardScroll)
    }


    /**
     * Updates the clipboard content displayed in the UI. This method retrieves the current clipboard content,
     * processes it, and dynamically updates the clipboard container and scroll view with the new data.
     *
     * @param clipboardManager The system ClipboardManager used to access the clipboard data.
     * @param clipboardContainer The container (LinearLayout) where clipboard content is displayed.
     * @param clipboardScroll The horizontal scroll view that holds the clipboard content and allows scrolling.
     */
    private fun updateClipboardContent(
        clipboardManager: ClipboardManager,
        clipboardContainer: LinearLayout,
        clipboardScroll: HorizontalScrollView
    ) {
        // Clear existing clipboard content in the UI to prepare for new content
        clipboardContainer.removeAllViews()

        // Retrieve the current clipboard data
        val clipboardData = clipboardManager.primaryClip
        Log.i(TAG, "Clipboard data: $clipboardData")

        // Check if there is any clipboard data
        if (clipboardData != null && clipboardData.itemCount > 0) {
            // Make the clipboard scroll view visible if there's data to display
            clipboardScroll.visibility = View.VISIBLE

            // Iterate over each item in the clipboard and add it to the UI
            for (i in 0 until clipboardData.itemCount) {
                val text = clipboardData.getItemAt(i).text
                Log.i(TAG, "Clipboard text: $text")

                // Check if the clipboard item contains valid text
                if (!text.isNullOrEmpty()) {
                    // Create a TextView to display the clipboard item
                    val textView = TextView(this).apply {
                        this.text = text
                        textSize = 16f
                        setPadding(16, 16, 16, 16)
                        maxLines = 2  // Limit the text to 2 lines
                        ellipsize = TextUtils.TruncateAt.END  // Add ellipsis for overflowing text
                        setOnClickListener {
                            // Insert the selected clipboard text into the current input connection
                            currentInputConnection?.commitText(this.text, 1)
                        }
                    }

                    Log.i(TAG, "Clipboard: adding to textview")
                    // Add the TextView to the clipboard container for display
                    clipboardContainer.addView(textView)
                }
            }
        } else {
            // Hide the clipboard UI if there is no clipboard data
            clipboardScroll.visibility = View.GONE
        }
    }



    /**
     * Sets up the autocomplete functionality by defining the `showSuggestions` function
     * inside the `setupAutocompleteLogic` function. It updates the autocomplete suggestions
     * based on the input text entered by the user.
     *
     * @param autocompleteArea The LinearLayout where autocomplete suggestions will be displayed.
     * @param suggestions The list of TextView elements where the suggestions will be shown.
     */
    private fun setupAutocompleteLogic(autocompleteArea: LinearLayout, suggestions: List<TextView>) {
        // Define showSuggestions function inside the setupAutocompleteLogic function
        // This function updates the autocomplete suggestions in the autocomplete area based on the input text
        fun showSuggestions(inputText: String) {
            // Call a function that updates the suggestions in the autocomplete area
            updateAutocompleteSuggestions(inputText, autocompleteArea, suggestions, this)
        }
    }



    /**
     * Configures the keyboard layout by setting up button listeners and logic for
     * various interactive features such as clipboard, autocomplete, numeric and alphabetic buttons,
     * backspace, action buttons, and the toggle button to switch between normal and special keyboard layouts.
     *
     * @param binding The binding object that provides access to the layout views, including the clipboard,
     *                autocomplete area, buttons, and special keys.
     */
    private fun setupKeyboardLayout(binding: KeyboardLayoutBinding) {
        // Extract views from the binding object for ease of access
        val clipboardScroll = binding.clipboardScroll
        val clipboardContainer = binding.clipboardContainer
        val autocompleteArea = binding.autocompleteArea
        val suggestion1 = binding.suggestion1
        val suggestion2 = binding.suggestion2
        val suggestion3 = binding.suggestion3

        // Initialize Clipboard Logic
        // This sets up the clipboard functionality where content from the clipboard is monitored and displayed
        setupClipboardLogic(clipboardScroll, clipboardContainer)

        // Initialize Autocomplete Logic
        // This sets up the autocomplete feature that provides suggestions based on user input
        setupAutocompleteLogic(autocompleteArea, listOf(suggestion1, suggestion2, suggestion3))

        // Setup all button listeners
        // Numeric buttons are set up for interaction
        setupButtonListeners(getNumericButtons())

        // Alphabetic buttons are set up for interaction
        setupButtonListeners(getAlphabeticButtons())

        // Setup backspace button to handle text deletion
        setupBackspaceButton(binding)

        // Setup action buttons like Enter, Space, Comma, and Dot
        // These buttons handle key actions, such as committing text, entering new lines, or inserting punctuation
        setupActionButtons(binding.root)

        // Setup the toggle button to switch between normal and special keyboard layouts
        // This allows the user to toggle between regular and special characters on the keyboard
        setupToggleSpecialKeysButton(binding)
    }



    /**
     * Toggles between the normal keyboard and special keys layouts when the special keys button is clicked.
     * It switches the keyboard layout between normal and special key configurations.
     */
    private fun setupToggleSpecialKeysButton(binding: KeyboardLayoutBinding) {
        // Retrieve the special keys toggle button from the binding
        val toggleButton = binding.functionalKeys.btnSpecialKeys

        // Set an onClickListener to toggle the layout when the button is pressed
        toggleButton.setOnClickListener {
            // Toggle the flag indicating whether special keys layout is enabled
            isSpecialKeysEnabled = !isSpecialKeysEnabled

            // Switch the keyboard layout based on the current state of special keys
            switchKeyboardLayout(toggleButton)
        }
    }


    /**
     * Switches the keyboard layout dynamically between normal and special keys based on the
     * isSpecialKeysEnabled flag. When the special keys layout is enabled, the layout is updated
     * to show special characters. Otherwise, the normal layout with standard characters is displayed.
     *
     * @param button The button that triggers the layout switch (usually a toggle button to switch between normal and special keyboard layouts).
     */
    private fun switchKeyboardLayout(button: Button) {
        // Determine which layout to use based on the isSpecialKeysEnabled flag
        activeView = if (isSpecialKeysEnabled) {
            // If special keys are enabled, configure the special keyboard layout
            setupKeyboardLayoutForSpecial(specialBinding)
            // Change the special keys toggle button text to indicate normal mode
            specialBinding.functionalKeys.btnSpecialKeys.text = getString(R.string.normal_mode_text)
            // Return the special layout root view
            specialBinding.root
        } else {
            // If special keys are not enabled, configure the normal keyboard layout
            setupKeyboardLayout(normalBinding)
            // Change the special keys toggle button text to indicate special mode
            normalBinding.functionalKeys.btnSpecialKeys.text = "!?"
            // Return the normal layout root view
            normalBinding.root
        }

        // Update the input view with the new active layout
        setInputView(activeView)
    }

    /**
     * Configures the special keyboard layout with special characters, numeric buttons,
     * and assigns touch listeners for the backspace button.
     *
     * @param specialBinding The binding for the special keyboard layout. It contains all the views for special keys.
     */
    private fun setupKeyboardLayoutForSpecial(specialBinding: KeyboardSpecialBinding) {

        // Array of special buttons and their corresponding special characters
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

        // Array of numeric buttons and their corresponding numeric characters
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

        // Setup the special character buttons
        setupSpecialCharacterButtons(specialButtons)

        // Setup the numeric buttons
        setupSpecialCharacterButtons(numericButtons)

        // Configure the Backspace button to handle single and continuous backspace actions
        specialBinding.backSpace.btnBackSpace.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // When the backspace button is pressed down, initiate a single backspace and start continuous backspace
                    isBackspacePressed = true
                    performSingleBackspace()
                    startContinuousBackspace()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // When the backspace button is released or the action is canceled, stop continuous backspace
                    isBackspacePressed = false
                    handler.removeCallbacksAndMessages(null) // Remove any ongoing backspace tasks
                }
            }
            true
        }

        // Setup other action buttons like Enter, Space, etc. in the special layout
        setupActionButtons(specialBinding.root)
    }

    /**
     * Sets up multiple buttons to commit their corresponding characters when clicked.
     *
     * @param buttons An array of pairs where each pair consists of a button and its corresponding character.
     *                When a button is clicked, the character is committed to the current input connection.
     */
    private fun setupSpecialCharacterButtons(buttons: Array<Pair<Button, String>>) {
        // Loop through each button and character pair
        buttons.forEach { (button, character) ->
            // Set click listener for each button to commit its character to the input connection
            button.setOnClickListener {
                currentInputConnection?.commitText(character, 1)
            }
        }
    }

    /**
     * Sets up multiple emoji buttons to commit their corresponding emoji characters when clicked.
     *
     * @param emojis An array of pairs where each pair consists of a TextView representing an emoji button
     *               and its corresponding emoji character.
     *               When an emoji button is clicked, the emoji is committed to the current input connection.
     */
    private fun setupEmojiButtons(emojis: Array<Pair<TextView, String>>) {
        // Loop through each emoji button and its character pair
        emojis.forEach { (emoji, character) ->
            // Set click listener for each emoji button to commit its emoji character to the input connection
            emoji.setOnClickListener {
                currentInputConnection?.commitText(character, 1)
            }
        }
    }

    /**
     * Handles the behavior of the backspace button.
     * When the backspace button is pressed, it deletes the last character.
     * If the button is held down, it continuously deletes characters until released.
     *
     * @param binding The binding of the keyboard layout that contains the backspace button.
     */
    private fun setupBackspaceButton(binding: KeyboardLayoutBinding) {
        // Set touch listener for the backspace button
        binding.rowAlphabetic3.backSpace.btnBackSpace.setOnTouchListener { _, event ->
            when (event.action) {
                // When the button is pressed down
                MotionEvent.ACTION_DOWN -> {
                    isBackspacePressed = true
                    performSingleBackspace() // Perform a single backspace
                    startContinuousBackspace() // Start continuous backspace if button is held down
                }
                // When the button is released or canceled
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isBackspacePressed = false
                    handler.removeCallbacksAndMessages(null) // Stop continuous backspace when button is released
                }
            }
            true
        }
    }

    /**
     * Configures action buttons like Enter, Space, Comma, and Dot for both layouts.
     * Sets up the respective functionality for action buttons on both the normal and special keyboard layouts.
     *
     * @param view The root view of the current keyboard layout (either normal or special layout).
     */
    private fun setupActionButtons(view: View) {
        val isNormalLayout = view == normalBinding.root

        if (isNormalLayout) {
            // Configure action buttons for the normal layout
            normalBinding.rowAlphabetic3.capsLock.btnCapsLock.setOnClickListener {
                toggleCapsLock() // Toggle between uppercase and lowercase letters
            }

            normalBinding.functionalKeys.btnEnter.setOnClickListener {
                currentInputConnection?.sendKeyEvent(
                    KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER) // Send an Enter key event
                )
            }

            normalBinding.functionalKeys.btnSpace.setOnClickListener {
                currentInputConnection?.commitText(" ", 1) // Commit a space character
                makeKeysLowercase() // Ensure keys are in lowercase after space
                val currentWord = getCurrentWord() // Get the current word typed
                if (isValidWord(currentWord)) {
                    saveUserInput(this, currentWord) // Save the word if it's valid
                }
            }

            normalBinding.functionalKeys.btnComma.setOnClickListener {
                currentInputConnection?.commitText(",", 1) // Commit a comma character
                makeKeysLowercase() // Ensure keys are in lowercase after comma
            }

            normalBinding.functionalKeys.btnDot.setOnClickListener {
                currentInputConnection?.commitText(".", 1) // Commit a dot character
                makeKeysLowercase() // Ensure keys are in lowercase after dot
            }
        } else {
            // Configure action buttons for the special layout
            specialBinding.functionalKeys.btnEnter.setOnClickListener {
                currentInputConnection?.sendKeyEvent(
                    KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER) // Send an Enter key event
                )
            }

            specialBinding.functionalKeys.btnSpace.setOnClickListener {
                currentInputConnection?.commitText(" ", 1) // Commit a space character
            }

            specialBinding.functionalKeys.btnComma.setOnClickListener {
                currentInputConnection?.commitText(",", 1) // Commit a comma character
            }

            specialBinding.functionalKeys.btnDot.setOnClickListener {
                currentInputConnection?.commitText(".", 1) // Commit a dot character
            }

            specialBinding.functionalKeys.btnSpecialKeys.setOnClickListener {
                isSpecialKeysEnabled = !isSpecialKeysEnabled // Toggle special keys enabled state
                switchKeyboardLayout(specialBinding.functionalKeys.btnSpecialKeys) // Switch keyboard layout
            }
        }
    }

    /**
     * Gets the current word being typed by extracting the text before the cursor.
     * It returns the first word before the cursor or an empty string if no text is found.
     *
     * @return The current word being typed, or an empty string if no text is present before the cursor.
     */
    private fun getCurrentWord(): String {
        // Get the text before the cursor (up to 100 characters)
        val textBeforeCursor = currentInputConnection?.getTextBeforeCursor(100, 0)?.toString() ?: ""
        Log.i(TAG, "current word: $textBeforeCursor") // Log the text before the cursor for debugging
        return textBeforeCursor.split(" ").firstOrNull() ?: "" // Return the first word or an empty string
    }

    /**
     * Toggles the Caps Lock state.
     * When Caps Lock is toggled, it updates the button's background and text/icon.
     * It also ensures that the alphabetic keys are updated to reflect the new Caps Lock state.
     */
    private fun toggleCapsLock() {
        Log.d(TAG, "toggleCapsLock: Caps Lock state = $isCapsLockActive") // Log the current Caps Lock state for debugging
        isCapsLockActive = !isCapsLockActive // Toggle the Caps Lock state

        // Choose the appropriate background for the Caps Lock button
        val iconRes = if (isCapsLockActive) R.drawable.upper_key_icon else R.drawable.lower_key_icon
        normalBinding.rowAlphabetic3.capsLock.btnCapsLock.setBackgroundResource(iconRes) // Set the button background

        // Update the text case for alphabetic buttons based on the Caps Lock state
        toggleCapsLockOnButtons(getAlphabeticButtons())
    }


    /**
     * Sends a single backspace key event to the input connection.
     * This simulates pressing the backspace key once.
     */
    private fun performSingleBackspace() {
        // Send a backspace key event (KEYCODE_DEL) to delete the character before the cursor
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
    }

    /**
     * Starts a continuous backspace operation while the backspace button is held down.
     * The backspace will be triggered repeatedly every 30ms as long as the button remains pressed.
     * It initiates the backspace action after a 200ms delay.
     */
    private fun startContinuousBackspace() {
        // Post a delayed runnable to repeatedly call backspace while the button is pressed
        handler.postDelayed(object : Runnable {
            override fun run() {
                // If the backspace button is still pressed, perform backspace
                if (isBackspacePressed) {
                    performSingleBackspace() // Perform backspace
                    handler.postDelayed(this, 30) // Repeat every 30ms
                }
            }
        }, 200) // Start continuous backspace after a 200ms delay
    }


    /**
     * Returns an array of numeric buttons that represent digits 0-9.
     * These buttons are part of the numeric keypad layout.
     */
    private fun getNumericButtons(): Array<Button> = arrayOf(
        normalBinding.rowNumeric.btn0, normalBinding.rowNumeric.btn1, normalBinding.rowNumeric.btn2,
        normalBinding.rowNumeric.btn3, normalBinding.rowNumeric.btn4, normalBinding.rowNumeric.btn5,
        normalBinding.rowNumeric.btn6, normalBinding.rowNumeric.btn7, normalBinding.rowNumeric.btn8,
        normalBinding.rowNumeric.btn9
    )

    /**
     * Returns an array of alphabetic buttons from the current view or binding.
     * This includes buttons for the letters from A to Z arranged in three rows.
     * If a custom view is provided, it will be used to fetch the alphabetic buttons.
     * Otherwise, it defaults to the current `normalBinding`.
     *
     * @param view An optional parameter for a custom view to get alphabetic buttons. Defaults to `null`.
     */
    private fun getAlphabeticButtons(view: View? = null): Array<Button> {
        // If no custom view is provided, use the default normalBinding
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
     * This function loops through each button in the provided array and sets a click listener that commits the corresponding text
     * (in uppercase or lowercase depending on the Caps Lock state) to the current input connection.
     *
     * @param buttons An array of buttons for which click listeners will be configured.
     */
    private fun setupButtonListeners(buttons: Array<Button>) {
        Log.i(TAG, "setupButtonListeners: Setting up buttons")
        buttons.forEach { button ->
            button.setOnClickListener {
                // Determine if the text should be in uppercase or lowercase based on Caps Lock state
                val inputText = if (isCapsLockActive) button.text.toString().uppercase() else button.text.toString().lowercase()
                currentInputConnection?.commitText(inputText, 1) // Commit the text to the input connection
                makeKeysLowercase() // Reset the case of keys if needed
            }
        }
    }

    /**
     * Toggles the Caps Lock state for all provided buttons.
     * This function updates the text of all buttons to reflect the current Caps Lock state (either uppercase or lowercase).
     *
     * @param buttons An array of buttons whose text will be updated based on the Caps Lock state.
     */
    private fun toggleCapsLockOnButtons(buttons: Array<Button>) {
        buttons.forEach { button ->
            // Update the text of the button based on the Caps Lock state
            button.text = if (isCapsLockActive) button.text.toString().uppercase() else button.text.toString().lowercase()
        }
    }

    /**
     * Disables Caps Lock and updates button states to reflect lowercase input.
     * This function sets `isCapsLockActive` to `false`, updates the button text to lowercase, and resets the Caps Lock button icon.
     */
    private fun makeKeysLowercase() {
        if (isCapsLockActive) {
            isCapsLockActive = false
            // Update the text on all alphabetic buttons to lowercase
            toggleCapsLockOnButtons(getAlphabeticButtons())
            // Reset the Caps Lock button icon to its default state
            normalBinding.rowAlphabetic3.capsLock.btnCapsLock.setBackgroundResource(R.drawable.lower_key_icon)
        }
    }
}
/**
 * Represents a Trie node which holds children nodes and a flag indicating if it's the end of a word.
 */
class TrieNode {
    val children = mutableMapOf<Char, TrieNode>() // A map of child nodes keyed by character.
    var isEndOfWord = false // Flag to indicate if this node marks the end of a word.
}

/**
 * Loads a dictionary of words from a ZIP file in the assets folder.
 * This function reads the ZIP file, extracts all words, and returns them as a list.
 *
 * @param context The context used to access the assets folder.
 * @return A list of words loaded from the dictionary.
 */
fun loadDictionaryFromAssets(context: Context): List<String> {
    val wordList = mutableListOf<String>()

    try {
        // Open the ZIP file from the assets folder
        context.assets.open("dictionary.zip").use { inputStream ->
            ZipInputStream(inputStream).use { zipStream ->
                var entry = zipStream.nextEntry
                Log.i("DictionaryLoader", "Loading dictionary: $entry")
                // Iterate through all entries in the ZIP file
                while (entry != null) {
                    if (!entry.isDirectory) {
                        // If the entry is a file (not a directory), read its content
                        BufferedReader(InputStreamReader(zipStream)).use { reader ->
                            reader.lineSequence().forEach { line ->
                                wordList.add(line.trim()) // Add each word to the list
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

/**
 * A Trie data structure to efficiently store and search for words.
 */
class Trie {
    private val root = TrieNode() // The root node of the Trie.

    /**
     * Inserts a word into the Trie.
     * For each character in the word, a new node is created if it doesn't already exist.
     * Marks the end of the word when the last character is inserted.
     *
     * @param word The word to be inserted into the Trie.
     */
    fun insert(word: String) {
        var node = root
        for (char in word) {
            node = node.children.getOrPut(char) { TrieNode() } // Create new node if not already present
        }
        node.isEndOfWord = true // Mark the end of the word
    }

    /**
     * Searches for all words that match the given prefix.
     * This function finds the node that corresponds to the end of the prefix and performs a depth-first search (DFS)
     * to collect all words that share this prefix.
     *
     * @param prefix The prefix to search for.
     * @return A list of words that match the given prefix.
     */
    fun searchByPrefix(prefix: String): List<String> {
        val result = mutableListOf<String>()
        var node = root

        // Navigate through the Trie to the end of the prefix
        for (char in prefix) {
            node = node.children[char] ?: return result // If the prefix is not found, return empty list
        }

        // Perform DFS to find all words with the given prefix
        dfs(node, prefix, result)
        return result
    }

    /**
     * Performs a depth-first search (DFS) to find all words that start with the given prefix.
     * It adds any complete words found during the search to the result list.
     *
     * @param node The current node in the Trie to start DFS.
     * @param prefix The current prefix being formed.
     * @param result The list to store the words found during DFS.
     */
    private fun dfs(node: TrieNode, prefix: String, result: MutableList<String>) {
        if (node.isEndOfWord) {
            result.add(prefix) // Add the word to the result if this node is the end of a word
        }
        // Recursively traverse the children nodes
        for ((char, child) in node.children) {
            dfs(child, prefix + char, result)
        }
    }
}

