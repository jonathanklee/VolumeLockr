package com.klee.volumelockr.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.klee.volumelockr.R
import com.klee.volumelockr.service.VolumeService

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val PASSWORD_PROTECTED_PREFERENCE = "password_protected"
        const val PASSWORD_CHANGE_PREFERENCE = "password"
        const val ALLOW_LOWER_PREFERENCE = "allow_lower"
        const val DELAY_IN_MS = 100L
        const val MIN_PASSWORD_LENGTH = 6
        private const val ENCRYPTED_PREFS_FILE = "secure_settings"
    }

    private var encryptedPrefs: SharedPreferences? = null

    private lateinit var passwordProtected: SwitchPreferenceCompat
    private lateinit var passwordChange: Preference
    private lateinit var shouldAllowLower: SwitchPreferenceCompat

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        initializeEncryptedPrefs()

        shouldAllowLower = findPreference(ALLOW_LOWER_PREFERENCE)!!
        passwordChange = findPreference(PASSWORD_CHANGE_PREFERENCE)!!
        passwordProtected = findPreference(PASSWORD_PROTECTED_PREFERENCE)!!

        shouldAllowLower.setOnPreferenceChangeListener { preferences, _ ->
            VolumeService.start(preferences.context)
            true
        }

        passwordChange.isEnabled = !passwordProtected.isChecked
        passwordChange.setOnPreferenceClickListener {
            showChangePasswordDialog()
            true
        }

        passwordProtected.setOnPreferenceChangeListener { _, value ->
            if (value == true) {
                passwordChange.isEnabled = false
            } else {
                askForPassword()
            }
            true
        }
        passwordProtected.isEnabled = isPasswordSet()
    }

    private fun initializeEncryptedPrefs() {
        try {
            val masterKey = MasterKey.Builder(requireContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            encryptedPrefs = EncryptedSharedPreferences.create(
                requireContext(),
                ENCRYPTED_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Toast.makeText(context, R.string.password_save_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun showChangePasswordDialog() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_password, null)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.password_input_layout)
        val editText = view.findViewById<EditText>(android.R.id.edit)

        editText.setOnFocusChangeListener { _, _ ->
            editText.postDelayed({ showKeyboard(editText) }, DELAY_IN_MS)
        }
        editText.requestFocus()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.change_password))
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val password = editText.text.toString()
                val validationError = validatePassword(password)

                if (validationError != null) {
                    inputLayout.error = validationError
                } else {
                    inputLayout.error = null
                    if (savePassword(password)) {
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun validatePassword(password: String): String? {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return getString(R.string.password_too_short, MIN_PASSWORD_LENGTH)
        }
        return null
    }

    private fun savePassword(newPassword: String): Boolean {
        val prefs = encryptedPrefs
        if (prefs == null) {
            Toast.makeText(context, R.string.password_save_error, Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            prefs.edit()
                .putString(PASSWORD_CHANGE_PREFERENCE, newPassword)
                .apply()
            passwordProtected.isEnabled = newPassword.isNotEmpty()
            true
        } catch (e: Exception) {
            Toast.makeText(context, R.string.password_save_error, Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun getStoredPassword(): String {
        return encryptedPrefs?.getString(PASSWORD_CHANGE_PREFERENCE, "") ?: ""
    }

    private fun askForPassword() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_password, null)
        val editText = view.findViewById<EditText>(android.R.id.edit)

        editText.setOnFocusChangeListener { _, _ ->
            editText.postDelayed({ showKeyboard(editText) }, DELAY_IN_MS)
        }
        editText.requestFocus()

        MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_lock)
            .setTitle(getString(R.string.enter_password))
            .setCancelable(false)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                checkPassword(editText.text.toString())
            }
            .show()
    }

    private fun showKeyboard(view: View) {
        val service = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        service.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun checkPassword(challenger: String) {
        val storedPassword = getStoredPassword()
        val isCorrect = storedPassword == challenger
        passwordProtected.isChecked = !isCorrect
        passwordChange.isEnabled = isCorrect
    }

    private fun isPasswordSet(): Boolean {
        return getStoredPassword().isNotEmpty()
    }
}
