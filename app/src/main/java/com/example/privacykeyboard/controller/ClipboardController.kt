package com.example.privacykeyboard.controller

import android.content.ClipboardManager
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.privacykeyboard.R

class ClipboardController(
    private val context: Context,
    private val clipboardScroll: ScrollView,
    private val clipboardContainer: LinearLayout,
    private val onTextSelected: (CharSequence) -> Unit
) {
    private val TAG = "ClipboardController"
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null

    fun setup() {
        listener = ClipboardManager.OnPrimaryClipChangedListener {
            Log.i(TAG, "Clipboard content changed")
            // Only refresh the content; show/hide is driven by the service
            refreshContent()
        }
        clipboardManager.addPrimaryClipChangedListener(listener!!)
    }

    fun cleanup() {
        listener?.let { clipboardManager.removePrimaryClipChangedListener(it) }
    }

    /** Show the strip (refreshes content first; stays GONE if clipboard is empty). */
    fun show() {
        refreshContent()
    }

    /** Hide the strip unconditionally. */
    fun hide() {
        clipboardScroll.visibility = View.GONE
    }

    // -----------------------------------------------------------------------

    private fun refreshContent() {
        clipboardContainer.removeAllViews()
        val clipboardData = clipboardManager.primaryClip
        Log.i(TAG, "Clipboard data: $clipboardData")

        if (clipboardData != null && clipboardData.itemCount > 0) {
            var hasContent = false
            for (i in 0 until clipboardData.itemCount) {
                val text = clipboardData.getItemAt(i).text
                if (!text.isNullOrBlank()) {
                    hasContent = true
                    val tv = TextView(context).apply {
                        this.text = text
                        textSize = 13f
                        setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                        maxLines = 1
                        ellipsize = TextUtils.TruncateAt.END
                        background = ContextCompat.getDrawable(context, R.drawable.ripple_effect)
                        isClickable = true
                        isFocusable = true
                        setOnClickListener { onTextSelected(this.text) }
                    }
                    clipboardContainer.addView(tv)
                    Log.i(TAG, "Clipboard: added item $i")
                }
            }
            clipboardScroll.visibility = if (hasContent) View.VISIBLE else View.GONE
        } else {
            clipboardScroll.visibility = View.GONE
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * context.resources.displayMetrics.density).toInt()
}
