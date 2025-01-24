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

class MyKeyboard : InputMethodService() {
    private var isCapsLockActive: Boolean = true // Tracks Caps Lock state
    private lateinit var normalBinding: KeyboardLayoutBinding // Binding for normal keyboard layout
    private lateinit var emojiBinding: EmojiLayoutBinding // Binding for normal keyboard layout
    private lateinit var specialBinding: KeyboardSpecialBinding // Binding for special keys layout
    private var activeView: View? = null // Currently active view
    private val handler: Handler = Handler(Looper.getMainLooper()) // Handler for backspace repetition
    private var isBackspacePressed: Boolean = false // Tracks if backspace is pressed
    private var isSpecialKeysEnabled: Boolean = false // Tracks Special Keys layout state
    private val TAG: String = "MyKeyboard" // Log tag for debugging
    private lateinit var keyboardRowsContainer: LinearLayout

    override fun onCreateInputView(): View {
        // Inflate normal and special layouts
        normalBinding = KeyboardLayoutBinding.inflate(layoutInflater)
        specialBinding = KeyboardSpecialBinding.inflate(layoutInflater)

        // Inflate emoji layout and attach it to the normal layout container
        emojiBinding = EmojiLayoutBinding.inflate(layoutInflater)
        normalBinding.normalContainer.addView(emojiBinding.root)
        // Initialize the container for keyboard rows
        keyboardRowsContainer = normalBinding.keyboardRowsContainer
        // Initially hide the emoji layout
        emojiBinding.root.visibility = View.GONE

        keyboardRowsContainer.visibility = View.VISIBLE
        // Set initial layout to the normal keyboard
        activeView = normalBinding.root
        setupKeyboardLayout(normalBinding)

        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

// Set the ScrollView height to around 1/4th of the screen height (for the emoji keyboard)
        val keyboardHeight = (screenHeight * 0.3).toInt() // Adjust this factor as needed
        val scrollView = emojiBinding.scrollView
        scrollView.layoutParams.height = keyboardHeight
        // Set up the emoji button
        normalBinding.btnEmoji.setOnClickListener {
            toggleEmojiLayout()
        }

        return activeView!!
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
                    textSize = 18f
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
                updateAutocompleteSuggestions(inputText, autocompleteArea, listOf(suggestion1, suggestion2, suggestion3))

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

    private fun updateAutocompleteSuggestions(inputText: String, autocompleteArea: LinearLayout, suggestions: List<TextView>) {
        val dummyWords = listOf("example", "keyboard", "autocomplete") // Replace with real suggestions
        // Get the last word or segment of the input text
        val lastInputText = inputText.split(" ").lastOrNull() ?: ""
        Log.i(TAG, "updateAutocompleteSuggestions: lastInputText $lastInputText")
        if (lastInputText.isNotEmpty()){
            val filteredWords = dummyWords.filter { word ->
                // Check if the last word in inputText contains the current word (case-insensitive)
                word.contains(lastInputText, ignoreCase = true)
            }
            Log.i(TAG, "from updateAutocompleteSuggestions: $inputText and $filteredWords, $dummyWords")
            // Update suggestions dynamically
            for (i in suggestions.indices) {
                if (i < filteredWords.size) {
                    suggestions[i].text = filteredWords[i]
                    suggestions[i].visibility = View.VISIBLE
                } else {
                    suggestions[i].visibility = View.GONE
                }
            }
            autocompleteArea.visibility = if (filteredWords.isEmpty()) View.GONE else View.VISIBLE
        }
    }


    private fun setupClipboardLogic(
        clipboardScroll: HorizontalScrollView,
        clipboardContainer: LinearLayout,

    ) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        Log.i(TAG, "clipboard Manager: $clipboardManager")
//        btnCancelClipboard.setOnClickListener {
//            clipboardScroll.visibility = View.GONE
//            btnCancelClipboard.visibility = View.GONE
//            clipboardContainer.removeAllViews()
//        }

        val clipboardData = clipboardManager.primaryClip
        Log.i(TAG, "clipboard data: $clipboardData, ${clipboardData?.itemCount}, ${clipboardContainer.childCount}")
        if (clipboardData != null && clipboardContainer.childCount ==1) {

            clipboardScroll.visibility = View.VISIBLE

            for (i in 0 until clipboardData.itemCount) {
                val text = clipboardData.getItemAt(i).text
                Log.i(TAG, "setupClipboardLogic: text $text")
                if (!text.isNullOrEmpty()) {
                    val textView = TextView(this).apply {
                        this.text = text
                        setOnClickListener {
                            currentInputConnection?.commitText(this.text, 1)
                        }
                    }
                    clipboardContainer.addView(textView)
                }
            }
        }
    }

    private fun setupAutocompleteLogic(autocompleteArea: LinearLayout, suggestions: List<TextView>) {
        val dummyWords = listOf("example", "keyboard", "autocomplete") // Replace with real suggestions

        fun showSuggestions(inputText: String) {
            Log.i(TAG, "is empty : ${inputText.isEmpty()}")
            val filteredWords = dummyWords.filter { it.startsWith(inputText, ignoreCase = true) }
            Log.i(TAG, "showSuggestions: $filteredWords")
            for (i in suggestions.indices) {
                if (i < filteredWords.size) {
                    suggestions[i].text = filteredWords[i]
                    suggestions[i].visibility = View.VISIBLE
                    suggestions[i].setOnClickListener {
                        currentInputConnection?.commitText(filteredWords[i], 1)
                    }
                } else {
                    suggestions[i].visibility = View.GONE
                }
            }
            autocompleteArea.visibility = if (filteredWords.isEmpty()) View.GONE else View.VISIBLE
        }

        // Example: Attach this function to input text change events
        val exampleInputWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.i(TAG, "onTextChanged: called ${s.toString()}")
                showSuggestions(s.toString())
            }
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
