package com.example.privacykeyboard.controller

import android.content.ClipboardManager
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class ClipboardController(
    private val context: Context,
    private val clipboardScroll: HorizontalScrollView,
    private val clipboardContainer: LinearLayout,
    private val onTextSelected: (CharSequence) -> Unit
) {
    private val TAG = "ClipboardController"
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var listener: ClipboardManager.OnPrimaryClipChangedListener? = null

    fun setup() {
        listener = ClipboardManager.OnPrimaryClipChangedListener {
            Log.i(TAG, "Clipboard content changed")
            updateUI()
        }
        clipboardManager.addPrimaryClipChangedListener(listener!!)
        updateUI()
    }

    fun cleanup() {
        listener?.let { clipboardManager.removePrimaryClipChangedListener(it) }
    }

    private fun updateUI() {
        clipboardContainer.removeAllViews()
        val clipboardData = clipboardManager.primaryClip
        Log.i(TAG, "Clipboard data: $clipboardData")

        if (clipboardData != null && clipboardData.itemCount > 0) {
            clipboardScroll.visibility = View.VISIBLE
            for (i in 0 until clipboardData.itemCount) {
                val text = clipboardData.getItemAt(i).text
                Log.i(TAG, "Clipboard text: $text")
                if (!text.isNullOrEmpty()) {
                    val textView = TextView(context).apply {
                        this.text = text
                        textSize = 16f
                        setPadding(16, 16, 16, 16)
                        maxLines = 2
                        ellipsize = TextUtils.TruncateAt.END
                        setOnClickListener { onTextSelected(this.text) }
                    }
                    Log.i(TAG, "Clipboard: adding to textview")
                    clipboardContainer.addView(textView)
                }
            }
        } else {
            clipboardScroll.visibility = View.GONE
        }
    }
}
