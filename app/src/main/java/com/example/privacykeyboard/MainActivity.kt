package com.example.privacykeyboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.example.privacykeyboard.databinding.ActivityMainBinding
import com.example.privacykeyboard.util.AppUpdateHelper

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
        if (enabled) {
            binding.tvStatus.text = "✓  Privacy Keyboard is enabled"
            binding.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
            binding.btnEnableKeyboard.isEnabled = false
            binding.btnEnableKeyboard.alpha = 0.45f
        } else {
            binding.tvStatus.text = "✗  Privacy Keyboard is not enabled"
            binding.tvStatus.setTextColor(Color.parseColor("#C62828"))
            binding.btnEnableKeyboard.isEnabled = true
            binding.btnEnableKeyboard.alpha = 1f
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
}
