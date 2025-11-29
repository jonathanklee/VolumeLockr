package com.klee.volumelockr.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.klee.volumelockr.R
import com.klee.volumelockr.service.VolumeService

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val PASSWORD_PROTECTED_PREFERENCE = "password_protected"
        const val PASSWORD_CHANGE_PREFERENCE = "password"
        const val ALLOW_LOWER_PREFERENCE = "allow_lower"
        const val DELAY_IN_MS = 100L
    }

    private lateinit var passwordProtected: SwitchPreferenceCompat
    private lateinit var passwordChange: EditTextPreference
    private lateinit var shouldAllowLower: SwitchPreferenceCompat

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        shouldAllowLower = findPreference(ALLOW_LOWER_PREFERENCE)!!
        passwordChange = findPreference(PASSWORD_CHANGE_PREFERENCE)!!
        passwordProtected = findPreference(PASSWORD_PROTECTED_PREFERENCE)!!

        shouldAllowLower.setOnPreferenceChangeListener { preferences, _ ->
            VolumeService.start(preferences.context)
            true
        }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferenceList = listView
        val startPadding = preferenceList.paddingLeft
        val topPadding = preferenceList.paddingTop
        val endPadding = preferenceList.paddingRight
        val bottomPadding = preferenceList.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(preferenceList) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            v.setPadding(
                startPadding + bars.left,
                topPadding + bars.top,
                endPadding + bars.right,
                bottomPadding + bars.bottom
            )

            WindowInsetsCompat.CONSUMED
        }
    }

    private fun askForPassword() {
        val editText = EditText(context)
        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        editText.setOnFocusChangeListener { _, _ ->
            editText.postDelayed({ showKeyboard(editText) }, DELAY_IN_MS)
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
            PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
                PASSWORD_CHANGE_PREFERENCE,
                ""
            )

        val isOk = password == challenger
        passwordProtected.isChecked = !isOk
        passwordChange.isEnabled = isOk
    }

    private fun isPasswordSet(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(requireContext()).getString(
            PASSWORD_CHANGE_PREFERENCE,
            ""
        )?.isNotEmpty()!!
    }
}
