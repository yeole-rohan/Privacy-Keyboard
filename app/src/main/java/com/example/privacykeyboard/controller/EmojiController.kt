package com.example.privacykeyboard.controller

import android.content.Context
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
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
    private var currentCategoryIndex = 0
    private val categoryNames = EmojiData.categoryNames

    fun setup() {
        setupCategoryButtons()
        setupSwipeGesture()
        populateRecent()
    }

    fun populateRecent() {
        emojiBinding.emojiContainer.removeAllViews()
        val recentEmojis = emojiRepo.loadRecent()
        if (recentEmojis.isEmpty()) {
            val tv = TextView(context).apply {
                text = context.getString(R.string.no_recent_emoji_found)
                textSize = 25f
                setPadding(16, 16, 16, 16)
                gravity = Gravity.CENTER
            }
            emojiBinding.emojiContainer.addView(tv)
            return
        }
        buildEmojiRows(recentEmojis)
    }

    fun populateCategory(name: String) {
        emojiBinding.emojiContainer.removeAllViews()
        val emojis = EmojiData.byCategory[name] ?: return
        buildEmojiRows(emojis)
    }

    private fun setupCategoryButtons() {
        emojiBinding.btnRecent.setOnClickListener {
            currentCategoryIndex = categoryNames.indexOf("Recent")
            populateRecent()
        }
        emojiBinding.btnSmileys.setOnClickListener {
            currentCategoryIndex = categoryNames.indexOf("Smileys")
            populateCategory("Smileys")
        }
        emojiBinding.btnAnimals.setOnClickListener {
            currentCategoryIndex = categoryNames.indexOf("Animals")
            populateCategory("Animals")
        }
        emojiBinding.btnFood.setOnClickListener {
            currentCategoryIndex = categoryNames.indexOf("Food")
            populateCategory("Food")
        }
        emojiBinding.btnActivity.setOnClickListener {
            currentCategoryIndex = categoryNames.indexOf("Activities")
            populateCategory("Activities")
        }
        emojiBinding.btnObjects.setOnClickListener {
            currentCategoryIndex = categoryNames.indexOf("Objects")
            populateCategory("Objects")
        }
        emojiBinding.btnSymbols.setOnClickListener {
            currentCategoryIndex = categoryNames.indexOf("Symbols")
            populateCategory("Symbols")
        }
        emojiBinding.btnFlags.setOnClickListener {
            currentCategoryIndex = categoryNames.indexOf("Flags")
            populateCategory("Flags")
        }
    }

    private fun setupSwipeGesture() {
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val deltaX = e2.x - (e1?.x ?: e2.x)
                if (Math.abs(deltaX) > 100 && Math.abs(velocityX) > 100) {
                    currentCategoryIndex = if (deltaX < 0)
                        (currentCategoryIndex + 1) % categoryNames.size
                    else
                        (currentCategoryIndex - 1 + categoryNames.size) % categoryNames.size
                    val category = categoryNames[currentCategoryIndex]
                    if (category == "Recent") populateRecent() else populateCategory(category)
                    return true
                }
                return false
            }
        })
        emojiBinding.scrollView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
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
                    setPadding(0, 8, 0, 8)
                }
                rowEmojis.forEach { rowEmoji ->
                    val emojiView = TextView(context).apply {
                        text = rowEmoji
                        textSize = 39f
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                        gravity = Gravity.CENTER
                        setPadding(8, 8, 8, 8)
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
}
