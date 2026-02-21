package com.rohanyeole.privacykeyboard

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rohanyeole.privacykeyboard.databinding.ActivityInfoBinding

class InfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val type = intent.getStringExtra("type") ?: "about"
        supportActionBar?.apply {
            title = titleFor(type)
            setDisplayHomeAsUpEnabled(true)
        }

        renderContent(type)

        binding.btnWebsite.visibility = View.VISIBLE
        binding.btnWebsite.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://privacy-keyboard.rohanyeole.com")))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    // -----------------------------------------------------------------------
    // Content routing
    // -----------------------------------------------------------------------

    private fun titleFor(type: String) = when (type) {
        "privacy" -> "Privacy Policy"
        "terms"   -> "Terms of Service"
        "legal"   -> "Legal"
        else      -> "About"
    }

    private fun renderContent(type: String) {
        val c = binding.contentContainer
        when (type) {
            "privacy" -> privacyContent(c)
            "terms"   -> termsContent(c)
            "legal"   -> legalContent(c)
            else      -> aboutContent(c)
        }
    }

    // -----------------------------------------------------------------------
    // Page content
    // -----------------------------------------------------------------------

    private fun aboutContent(c: LinearLayout) {
        addTitle(c, "Privacy Keyboard")
        addCaption(c, "Version 1.0  ·  Free & private")
        addSpacer(c, 12)
        addBody(c, "A free keyboard that works entirely on your device. Your typing stays on your phone — nothing is ever sent anywhere.")
        addSpacer(c, 16)
        addSectionHeader(c, "Features")
        addBullet(c, "Suggests words as you type")
        addBullet(c, "Remembers words you use often")
        addBullet(c, "Emoji picker with your recent favourites")
        addBullet(c, "Shows items you have copied, ready to paste")
        addBullet(c, "Tap Shift once for a capital letter, tap twice to lock capitals")
        addBullet(c, "Gentle vibration when you press keys")
        addBullet(c, "Multiple colour themes to match your style")
        addBullet(c, "Slide the spacebar left or right to move the cursor")
        addSpacer(c, 16)
        addSectionHeader(c, "About the author")
        addBody(c, "Made by Rohan Yeole")
        addCaption(c, "rohanyeole.com")
    }

    private fun privacyContent(c: LinearLayout) {
        addTitle(c, "Privacy Policy")
        addCaption(c, "Last updated: February 2026")
        addSpacer(c, 12)
        addBody(c, "Privacy Keyboard is built to protect your privacy. Everything stays on your device — nothing is collected, shared, or sent anywhere.")
        addSpacer(c, 16)
        addSectionHeader(c, "What is saved on your device")
        addNumberedItem(c, "1", "Your personal word list", "Words you type are saved on your phone to make suggestions better over time. This never leaves your device.")
        addSpacer(c, 6)
        addNumberedItem(c, "2", "Your recent emoji", "Emoji you use often are remembered so they appear at the top next time. This never leaves your device.")
        addSpacer(c, 6)
        addNumberedItem(c, "3", "Your settings", "Your vibration and theme choices are saved on your device.")
        addSpacer(c, 6)
        addNumberedItem(c, "4", "Clipboard items", "Items you copy are shown while the keyboard is open. They are not saved to storage and disappear when you close the keyboard.")
        addSpacer(c, 16)
        addSectionHeader(c, "What we do NOT do")
        addCrossItem(c, "We never record or transmit what you type")
        addCrossItem(c, "No usage statistics or analytics")
        addCrossItem(c, "No advertising or ad tracking")
        addCrossItem(c, "No crash reports sent anywhere")
        addCrossItem(c, "No internet connection required or used")
        addSpacer(c, 16)
        addSectionHeader(c, "Questions?")
        addBody(c, "We are happy to answer any privacy questions. Reach us through our website.")
    }

    private fun termsContent(c: LinearLayout) {
        addTitle(c, "Terms of Service")
        addCaption(c, "Last updated: February 2026")
        addSpacer(c, 12)
        addBody(c, "By using Privacy Keyboard you agree to the following terms. They are written to be simple and fair.")
        addSpacer(c, 16)
        addSectionHeader(c, "Free to use")
        addBody(c, "Privacy Keyboard is free for personal use. You may not sell or distribute modified versions of this app without crediting the original author.")
        addSpacer(c, 12)
        addSectionHeader(c, "No guarantees")
        addBody(c, "The app is provided as-is. We do our best to make it work well, but we cannot promise it will work perfectly on every device or that suggestions will always be accurate.")
        addSpacer(c, 12)
        addSectionHeader(c, "Responsibility")
        addBody(c, "We are not responsible for any issues that arise from your use of the app.")
        addSpacer(c, 12)
        addSectionHeader(c, "Updates to these terms")
        addBody(c, "We may update these terms occasionally. Continuing to use the app means you accept any updated terms.")
        addSpacer(c, 12)
        addSectionHeader(c, "Governing law")
        addBody(c, "These terms are governed by the laws of the country where the author resides.")
    }

    private fun legalContent(c: LinearLayout) {
        addTitle(c, "Legal Notices")
        addSpacer(c, 8)
        addBody(c, "Privacy Keyboard is built using open-source software. We are grateful to the following projects and their creators.")
        addSpacer(c, 16)
        addSectionHeader(c, "Kotlin")
        addBody(c, "© JetBrains s.r.o.")
        addCaption(c, "Apache License 2.0  ·  kotlinlang.org")
        addSpacer(c, 10)
        addSectionHeader(c, "Android Jetpack")
        addBody(c, "© Google LLC")
        addCaption(c, "Apache License 2.0  ·  developer.android.com/jetpack")
        addSpacer(c, 10)
        addSectionHeader(c, "Material Design Components")
        addBody(c, "© Google LLC")
        addCaption(c, "Apache License 2.0  ·  material.io/components")
        addSpacer(c, 10)
        addSectionHeader(c, "Roboto Font")
        addBody(c, "© Google LLC")
        addCaption(c, "Apache License 2.0")
        addSpacer(c, 16)
        addBody(c, "Full license text: apache.org/licenses/LICENSE-2.0")
        addSpacer(c, 16)
        addSectionHeader(c, "Privacy Keyboard")
        addBody(c, "An independent project by Rohan Yeole.")
        addCaption(c, "rohanyeole.com")
    }

    // -----------------------------------------------------------------------
    // Block renderers
    // -----------------------------------------------------------------------

    private fun addTitle(c: LinearLayout, text: String) {
        c.addView(TextView(this).apply {
            this.text = text
            textSize = 26f
            setTextColor(Color.parseColor("#1b1b1d"))
            setTypeface(null, Typeface.BOLD)
            layoutParams = rowParams(bottomDp = 2)
        })
    }

    private fun addCaption(c: LinearLayout, text: String) {
        c.addView(TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.parseColor("#888888"))
            setTypeface(null, Typeface.ITALIC)
            layoutParams = rowParams(bottomDp = 4)
        })
    }

    private fun addSectionHeader(c: LinearLayout, text: String) {
        // Colored label
        c.addView(TextView(this).apply {
            this.text = text.uppercase()
            textSize = 11f
            setTextColor(Color.parseColor("#4A90D9"))
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.1f
            layoutParams = rowParams(topDp = 4, bottomDp = 4)
        })
        // Thin accent line
        c.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#4A90D9"))
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(2)).also { it.bottomMargin = dp(8) }
        })
    }

    private fun addBody(c: LinearLayout, text: String) {
        c.addView(TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.parseColor("#333333"))
            setLineSpacing(0f, 1.5f)
            layoutParams = rowParams(bottomDp = 4)
        })
    }

    private fun addBullet(c: LinearLayout, text: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = rowParams(bottomDp = 5)
        }
        row.addView(TextView(this).apply {
            this.text = "•"
            textSize = 15f
            setTextColor(Color.parseColor("#4A90D9"))
            layoutParams = LinearLayout.LayoutParams(dp(20), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        row.addView(TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.parseColor("#333333"))
            setLineSpacing(0f, 1.5f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        c.addView(row)
    }

    private fun addCrossItem(c: LinearLayout, text: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = rowParams(bottomDp = 5)
        }
        row.addView(TextView(this).apply {
            this.text = "✗"
            textSize = 14f
            setTextColor(Color.parseColor("#EF5350"))
            layoutParams = LinearLayout.LayoutParams(dp(22), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        row.addView(TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.parseColor("#333333"))
            setLineSpacing(0f, 1.5f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        c.addView(row)
    }

    private fun addNumberedItem(c: LinearLayout, num: String, heading: String, detail: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = rowParams(bottomDp = 2)
        }
        row.addView(TextView(this).apply {
            text = num
            textSize = 13f
            setTextColor(Color.parseColor("#4A90D9"))
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(dp(22), LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        col.addView(TextView(this).apply {
            text = heading
            textSize = 15f
            setTextColor(Color.parseColor("#1b1b1d"))
            setTypeface(null, Typeface.BOLD)
        })
        col.addView(TextView(this).apply {
            text = detail
            textSize = 13f
            setTextColor(Color.parseColor("#666666"))
            setLineSpacing(0f, 1.4f)
        })
        row.addView(col)
        c.addView(row)
    }

    private fun addSpacer(c: LinearLayout, heightDp: Int) {
        c.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)
            )
        })
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun rowParams(topDp: Int = 0, bottomDp: Int = 0) = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).also {
        it.topMargin = dp(topDp)
        it.bottomMargin = dp(bottomDp)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
