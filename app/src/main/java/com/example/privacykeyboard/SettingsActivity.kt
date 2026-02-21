package com.example.privacykeyboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.privacykeyboard.data.KeyboardPreferences
import com.example.privacykeyboard.data.KeyboardTheme
import com.example.privacykeyboard.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: KeyboardPreferences

    // Maps themeId → its checkmark TextView so we can update all at once on selection
    private val checkViews = mutableMapOf<String, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.apply {
            title = "Settings"
            setDisplayHomeAsUpEnabled(true)
        }
        prefs = KeyboardPreferences(this)
        setupVibration()
        setupThemes()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }

    // -----------------------------------------------------------------------
    // Vibration
    // -----------------------------------------------------------------------

    private fun setupVibration() {
        binding.switchVibration.isChecked = prefs.vibrationEnabled
        binding.layoutStrength.visibility = if (prefs.vibrationEnabled) View.VISIBLE else View.GONE

        binding.switchVibration.setOnCheckedChangeListener { _, checked ->
            prefs.vibrationEnabled = checked
            binding.layoutStrength.visibility = if (checked) View.VISIBLE else View.GONE
        }

        val checkId = when (prefs.vibrationStrength) {
            "light"  -> R.id.rbLight
            "strong" -> R.id.rbStrong
            else     -> R.id.rbMedium
        }
        binding.rgStrength.check(checkId)
        applyStrengthStyles()

        binding.rgStrength.setOnCheckedChangeListener { _, id ->
            prefs.vibrationStrength = when (id) {
                R.id.rbLight  -> "light"
                R.id.rbStrong -> "strong"
                else          -> "medium"
            }
            applyStrengthStyles()
        }
    }

    private fun applyStrengthStyles() {
        listOf(binding.rbLight, binding.rbMedium, binding.rbStrong).forEach { rb ->
            if (rb.isChecked) {
                rb.setTypeface(null, Typeface.BOLD)
                rb.setTextColor(Color.parseColor("#1b1b1d"))
            } else {
                rb.setTypeface(null, Typeface.NORMAL)
                rb.setTextColor(Color.parseColor("#AAAAAA"))
            }
        }
    }

    // -----------------------------------------------------------------------
    // Themes
    // -----------------------------------------------------------------------

    private fun setupThemes() {
        checkViews.clear()
        KeyboardTheme.all.forEachIndexed { index, theme ->
            binding.themeListContainer.addView(buildThemeRow(theme))
            if (index < KeyboardTheme.all.lastIndex) {
                binding.themeListContainer.addView(makeRowDivider())
            }
        }
        updateThemePreview(KeyboardTheme.forId(prefs.themeId))
    }

    private fun makeRowDivider(): View {
        val dp = resources.displayMetrics.density
        return View(this).apply {
            setBackgroundColor(Color.parseColor("#11000000"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
            ).apply { marginStart = (52 * dp).toInt() }
        }
    }

    private fun buildThemeRow(theme: KeyboardTheme): View {
        val dp = resources.displayMetrics.density

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt())
        }

        // Color swatch: filled circle using kbBg with keyBg ring
        val swatchBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(theme.keyBg)
            setStroke((3 * dp).toInt(), theme.kbBg)
        }
        val swatch = View(this).apply {
            background = swatchBg
            layoutParams = LinearLayout.LayoutParams(
                (26 * dp).toInt(), (26 * dp).toInt()
            ).apply { marginEnd = (14 * dp).toInt() }
        }

        // Theme name
        val nameView = TextView(this).apply {
            text = theme.name
            setTextColor(Color.parseColor("#1b1b1d"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        // Checkmark (visible only for selected theme)
        val checkView = TextView(this).apply {
            text = "✓"
            setTextColor(Color.parseColor("#4A90D9"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(null, Typeface.BOLD)
            visibility = if (theme.id == prefs.themeId) View.VISIBLE else View.INVISIBLE
        }
        checkViews[theme.id] = checkView

        row.addView(swatch)
        row.addView(nameView)
        row.addView(checkView)

        row.setOnClickListener {
            prefs.themeId = theme.id
            checkViews.forEach { (id, cv) ->
                cv.visibility = if (id == theme.id) View.VISIBLE else View.INVISIBLE
            }
            updateThemePreview(theme)
        }
        return row
    }

    private fun updateThemePreview(theme: KeyboardTheme) {
        val dp = resources.displayMetrics.density
        binding.previewContainer.background = GradientDrawable().apply {
            setColor(theme.kbBg)
            cornerRadius = 8 * dp
        }
        applyThemeToPreviewView(binding.previewContainer, theme)
    }

    private fun applyThemeToPreviewView(view: View, theme: KeyboardTheme) {
        when {
            view is Button -> {
                view.backgroundTintList = ColorStateList.valueOf(theme.keyBg)
                view.setTextColor(theme.keyText)
            }
            view is ViewGroup -> {
                for (i in 0 until view.childCount) applyThemeToPreviewView(view.getChildAt(i), theme)
            }
        }
    }
}
