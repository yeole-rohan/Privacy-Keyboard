package com.rohanyeole.privacykeyboard.controller

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.rohanyeole.privacykeyboard.R

class ClipboardController(
    private val context: Context,
    private val chipsContainer: LinearLayout,
    private val onTextSelected: (CharSequence) -> Unit
) {
    private val TAG = "ClipboardController"
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null

    fun setup() {
        listener = ClipboardManager.OnPrimaryClipChangedListener {
            Log.i(TAG, "Clipboard content changed")
            refreshContent()
        }
        clipboardManager.addPrimaryClipChangedListener(listener!!)
    }

    fun cleanup() {
        listener?.let { clipboardManager.removePrimaryClipChangedListener(it) }
    }

    /** Show clipboard chips in the suggestion bar (stays GONE if clipboard is empty). */
    fun show() {
        refreshContent()
    }

    /** Hide and clear clipboard chips. */
    fun hide() {
        chipsContainer.removeAllViews()
        chipsContainer.visibility = View.GONE
    }

    // -----------------------------------------------------------------------

    private fun refreshContent() {
        chipsContainer.removeAllViews()
        val clipboardData = clipboardManager.primaryClip
        Log.i(TAG, "Clipboard data: $clipboardData")

        if (clipboardData == null || clipboardData.itemCount == 0) {
            chipsContainer.visibility = View.GONE
            return
        }

        var hasContent = false
        for (i in 0 until clipboardData.itemCount) {
            val fullText = clipboardData.getItemAt(i).text
            if (fullText.isNullOrBlank()) continue

            if (hasContent) {
                // Thin divider between chips
                val divider = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(dpToPx(1), LinearLayout.LayoutParams.MATCH_PARENT)
                    setBackgroundColor(0x22000000.toInt())
                }
                chipsContainer.addView(divider)
            }

            val preview = if (fullText.length > 7) "${fullText.take(7)}…" else fullText.toString()
            val chip = TextView(context).apply {
                text = preview
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                maxLines = 1
                setPadding(dpToPx(4), 0, dpToPx(4), 0)
                background = ContextCompat.getDrawable(context, R.drawable.ripple_effect)
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                setOnClickListener { onTextSelected(fullText) }
            }
            chipsContainer.addView(chip)
            hasContent = true
            Log.i(TAG, "Clipboard: added chip $i")

            // Limit to 3 chips
            if (i >= 2) break
        }

        chipsContainer.visibility = if (hasContent) View.VISIBLE else View.GONE
    }

    private fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()
}
