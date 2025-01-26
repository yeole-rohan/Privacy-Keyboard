package com.example.privacykeyboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.example.privacykeyboard.databinding.ActivityMainBinding

/**
 * MainActivity is the entry point of the app. It manages the UI related to enabling the custom keyboard
 * and opening the keyboard settings.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /**
     * Called when the activity is created. It initializes the view and sets up the button listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            // Check if the custom keyboard is enabled and update the button state accordingly
            if (isKeyboardEnabled()) {
                btnEnableKeyboard.isEnabled = false // Disable the button if the keyboard is already enabled
            }

            // Set up the click listener for enabling the keyboard
            btnEnableKeyboard.setOnClickListener {
                // If the keyboard is not enabled, open the settings to enable it
                if (!isKeyboardEnabled()) {
                    openKeyboardSettings()
                }
            }
        }
    }

    /**
     * Opens the input method settings page in the device settings.
     * This allows the user to enable or configure the keyboard.
     */
    private fun openKeyboardSettings() {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        startActivity(intent) // Start the activity for input method settings
    }

    /**
     * Checks if the custom keyboard is enabled in the device's input method settings.
     * It compares the enabled keyboard list with the custom keyboard's package name.
     *
     * @return true if the custom keyboard is enabled, false otherwise.
     */
    private fun isKeyboardEnabled(): Boolean {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // Get a list of enabled input methods (keyboards) by their IDs
        val enabledInputMethodIds = inputMethodManager.enabledInputMethodList.map { it.id }
        // Check if the custom keyboard's package name is in the list of enabled keyboards
        return enabledInputMethodIds.contains("com.example.privacykeyboard/.MyKeyBoard")
    }
}
