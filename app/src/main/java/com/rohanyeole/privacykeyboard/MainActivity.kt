package com.rohanyeole.privacykeyboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.rohanyeole.privacykeyboard.databinding.ActivityMainBinding
import com.rohanyeole.privacykeyboard.util.AppUpdateHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var updateHelper: AppUpdateHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupClickListeners()

        updateHelper = AppUpdateHelper(this)
        updateHelper.register()
        updateHelper.checkForUpdate()
    }

    override fun onResume() {
        super.onResume()
        updateKeyboardStatus()
        updateHelper.onResume()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        updateHelper.onActivityResult(requestCode, resultCode)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateHelper.unregister()
    }

    // -----------------------------------------------------------------------
    // Status
    // -----------------------------------------------------------------------

    private fun updateKeyboardStatus() {
        val enabled = isKeyboardEnabled()
        val active  = isKeyboardDefault()

        when {
            active -> {
                // Step 2 complete — fully set up
                binding.tvStatus.text = "✓  Active keyboard"
                binding.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                binding.tvStatusSub.text = "Privacy Keyboard is your active keyboard. You're all set!"

                binding.btnEnableKeyboard.isEnabled = false
                binding.btnEnableKeyboard.alpha = 0.4f

                binding.btnSelectKeyboard.isEnabled = false
                binding.btnSelectKeyboard.alpha = 0.4f
                binding.btnSelectKeyboard.setTextColor(Color.parseColor("#2E7D32"))
                binding.btnSelectKeyboard.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D32"))
                binding.btnSelectKeyboard.text = "✓  Set as active keyboard"
            }
            enabled -> {
                // Step 1 done — nudge towards step 2
                binding.tvStatus.text = "⚡  Enabled — not yet active"
                binding.tvStatus.setTextColor(Color.parseColor("#E65100"))
                binding.tvStatusSub.text = "Tap 'Select as Active Keyboard' below to finish setup."

                binding.btnEnableKeyboard.isEnabled = false
                binding.btnEnableKeyboard.alpha = 0.4f

                binding.btnSelectKeyboard.isEnabled = true
                binding.btnSelectKeyboard.alpha = 1f
                binding.btnSelectKeyboard.setTextColor(Color.WHITE)
                binding.btnSelectKeyboard.setBackgroundColor(Color.parseColor("#4A90D9"))
                binding.btnSelectKeyboard.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#4A90D9"))
                binding.btnSelectKeyboard.text = "Select as Active Keyboard"
            }
            else -> {
                // Step 1 not done
                binding.tvStatus.text = "✗  Privacy Keyboard is not enabled"
                binding.tvStatus.setTextColor(Color.parseColor("#C62828"))
                binding.tvStatusSub.text = "Tap 'Enable Privacy Keyboard' below, find it in the list, and toggle it on."

                binding.btnEnableKeyboard.isEnabled = true
                binding.btnEnableKeyboard.alpha = 1f

                binding.btnSelectKeyboard.isEnabled = false
                binding.btnSelectKeyboard.alpha = 0.4f
                binding.btnSelectKeyboard.setTextColor(Color.parseColor("#4A90D9"))
                binding.btnSelectKeyboard.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#4A90D9"))
                binding.btnSelectKeyboard.text = "Select as Active Keyboard"
            }
        }
    }

    // -----------------------------------------------------------------------
    // Listeners
    // -----------------------------------------------------------------------

    private fun setupClickListeners() {
        // Enable keyboard → opens input-method settings list
        binding.btnEnableKeyboard.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        // Select as default → shows system IME picker dialog
        binding.btnSelectKeyboard.setOnClickListener {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showInputMethodPicker()
        }

        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.cardAbout.setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java).putExtra("type", "about"))
        }
        binding.cardPrivacy.setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java).putExtra("type", "privacy"))
        }
        binding.cardTerms.setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java).putExtra("type", "terms"))
        }
        binding.cardLegal.setOnClickListener {
            startActivity(Intent(this, InfoActivity::class.java).putExtra("type", "legal"))
        }
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private fun isKeyboardEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.id.contains("PrivacyKeyboardService") }
    }

    private fun isKeyboardDefault(): Boolean {
        val default = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return default?.contains("PrivacyKeyboardService") == true
    }
}
