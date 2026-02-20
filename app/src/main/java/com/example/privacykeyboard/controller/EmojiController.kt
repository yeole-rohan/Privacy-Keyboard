package com.example.privacykeyboard.controller

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.privacykeyboard.R
import com.example.privacykeyboard.data.EmojiData
import com.example.privacykeyboard.data.EmojiRepository
import com.example.privacykeyboard.databinding.EmojiLayoutBinding

class EmojiController(
    private val context: Context,
    private val emojiBinding: EmojiLayoutBinding,
    private val emojiRepo: EmojiRepository,
    private val onEmojiSelected: (String) -> Unit
) {
    // Maps category name → Y offset of its section header (populated after layout pass)
    private val sectionOffsets = mutableListOf<Pair<String, Int>>()

    fun setup() {
        attachScrollListener()
    }

    /** Renders all categories in one continuous pass. Call each time the panel opens. */
    fun render() {
        emojiBinding.emojiContainer.removeAllViews()
        sectionOffsets.clear()

        // Recent first, then every named category (skip "Recent" from categoryNames)
        buildSection("Recent", getRecentEmojis())
        EmojiData.categoryNames
            .filter { it != "Recent" }
            .forEach { name ->
                buildSection(name, EmojiData.byCategory[name] ?: emptyList())
            }

        // After views are laid out, record each header's Y offset for scroll detection
        emojiBinding.emojiContainer.post { collectSectionOffsets() }

        // Scroll to top and prime the label
        emojiBinding.scrollView.scrollTo(0, 0)
        emojiBinding.tvCurrentCategory.text = "Recent"
    }

    // -----------------------------------------------------------------------
    // Private
    // -----------------------------------------------------------------------

    private fun attachScrollListener() {
        emojiBinding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val current = sectionOffsets.lastOrNull { it.second <= scrollY }?.first
                ?: sectionOffsets.firstOrNull()?.first
                ?: return@setOnScrollChangeListener
            emojiBinding.tvCurrentCategory.text = current
        }
    }

    private fun collectSectionOffsets() {
        sectionOffsets.clear()
        // Section headers are the direct children of emojiContainer that are TextViews
        // with a non-null tag set to the category name
        for (i in 0 until emojiBinding.emojiContainer.childCount) {
            val child = emojiBinding.emojiContainer.getChildAt(i)
            val tag = child.tag as? String ?: continue
            sectionOffsets.add(tag to child.top)
        }
    }

    private fun buildSection(name: String, emojis: List<String>) {
        // Section header
        val header = TextView(context).apply {
            text = name
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.08f
            alpha = 0.55f
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(2))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            tag = name  // used by collectSectionOffsets()
        }
        emojiBinding.emojiContainer.addView(header)

        if (emojis.isEmpty()) {
            val placeholder = TextView(context).apply {
                text = context.getString(R.string.no_recent_emoji_found)
                textSize = 14f
                setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                gravity = Gravity.CENTER
                alpha = 0.5f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            emojiBinding.emojiContainer.addView(placeholder)
            return
        }

        buildEmojiRows(emojis)
    }

    private fun buildEmojiRows(emojis: List<String>) {
        val rowEmojis = mutableListOf<String>()
        emojis.forEachIndexed { index, emoji ->
            rowEmojis.add(emoji)
            if (rowEmojis.size == 7 || index == emojis.lastIndex) {
                val rowLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(0, dpToPx(4), 0, dpToPx(4))
                }
                rowEmojis.forEach { rowEmoji ->
                    val emojiView = TextView(context).apply {
                        text = rowEmoji
                        textSize = 39f
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                        gravity = Gravity.CENTER
                        setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
                        background = ContextCompat.getDrawable(context, R.drawable.ripple_effect)
                        isClickable = true
                        isFocusable = true
                        setOnClickListener {
                            onEmojiSelected(rowEmoji)
                            emojiRepo.addToRecent(rowEmoji)
                        }
                    }
                    rowLayout.addView(emojiView)
                }
                emojiBinding.emojiContainer.addView(rowLayout)
                rowEmojis.clear()
            }
        }
    }

    private fun getRecentEmojis(): List<String> = emojiRepo.loadRecent()

    private fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()
}
