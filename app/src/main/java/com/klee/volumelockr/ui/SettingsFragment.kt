package com.klee.volumelockr.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.klee.volumelockr.R

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val PASSWORD_PROTECTED_PREFERENCE = "password_protected"
        const val PASSWORD_CHANGE_PREFERENCE = "password"
    }

    private lateinit var passwordProtected: SwitchPreferenceCompat
    private lateinit var passwordChange: EditTextPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        passwordChange = findPreference(PASSWORD_CHANGE_PREFERENCE)!!
        passwordProtected = findPreference(PASSWORD_PROTECTED_PREFERENCE)!!

        passwordChange.isEnabled = !passwordProtected.isChecked
        passwordChange.setOnBindEditTextListener { editText ->
            editText.text.clear()
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        passwordChange.setOnPreferenceChangeListener { _, value ->
            passwordProtected.isEnabled = value.toString().isNotEmpty()
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

    private fun askForPassword() {

        val editText = EditText(context)
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        editText.setOnFocusChangeListener { _, _ ->
            editText.postDelayed({ showKeyboard(editText) }, 100)
        }
        editText.requestFocus()

        AlertDialog.Builder(context)
            .setTitle(getString(R.string.enter_password))
            .setCancelable(false)
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                checkPassword(editText.text.toString())
            }
            .show()
    }

    private fun showKeyboard(view: View) {
        val service = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        service.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun checkPassword(challenger: String) {
        val password =
            PreferenceManager.getDefaultSharedPreferences(context).getString(
                PASSWORD_CHANGE_PREFERENCE, ""
            )

        val isOk = password == challenger
        passwordProtected.isChecked = !isOk
        passwordChange.isEnabled = isOk
    }

    private fun isPasswordSet(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
            PASSWORD_CHANGE_PREFERENCE, ""
        )?.length != 0
    }
}
