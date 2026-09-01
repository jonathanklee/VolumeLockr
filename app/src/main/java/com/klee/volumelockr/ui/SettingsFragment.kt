package com.klee.volumelockr.ui

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.klee.volumelockr.R
import com.klee.volumelockr.service.VolumeService
import java.io.IOException
import java.security.GeneralSecurityException
import androidx.core.content.edit
import androidx.core.os.BundleCompat
import android.view.ViewGroup

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        private const val TAG = "SettingsFragment"
        const val PASSWORD_PROTECTED_PREFERENCE = "password_protected"
        const val PASSWORD_CHANGE_PREFERENCE = "password"
        const val ALLOW_LOWER_PREFERENCE = "allow_lower"
        const val DELAY_IN_MS = 100L
        const val MIN_PASSWORD_LENGTH = 6
        private const val ENCRYPTED_PREFS_FILE = "secure_settings"

        const val USE_PRESETS_ALL_PREFERENCE = "use_presets_locking_all"
        const val USE_PRESETS_INDIVIDUAL_PREFERENCE = "use_presets_locking_individual"

        const val CHANGE_PRESETS_PREFERENCE = "change_presets"

        const val RING_PRESET_PREFERENCE = "ring_volume_preset"
        const val MEDIA_VOLUME_PRESET_PREFERENCE = "media_volume_preset"
        const val CALL_VOLUME_PRESET_PREFERENCE = "call_volume_preset"
        const val ALARM_VOLUME_PRESET_PREFERENCE = "alarm_volume_preset"
        const val NOTIFICATION_VOLUME_PRESET_PREFERENCE = "notification_volume_preset"
    }

    private var encryptedPrefs: SharedPreferences? = null

    private lateinit var passwordProtected: SwitchPreferenceCompat
    private lateinit var passwordChange: Preference
    private lateinit var shouldAllowLower: SwitchPreferenceCompat

    private lateinit var usePresetsWhenLockingAll: Preference
    private lateinit var changePresets: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        initializeEncryptedPrefs()

        shouldAllowLower = findPreference(ALLOW_LOWER_PREFERENCE)!!
        passwordChange = findPreference(PASSWORD_CHANGE_PREFERENCE)!!
        passwordProtected = findPreference(PASSWORD_PROTECTED_PREFERENCE)!!
        usePresetsWhenLockingAll = findPreference(USE_PRESETS_ALL_PREFERENCE)!!
        changePresets = findPreference(CHANGE_PRESETS_PREFERENCE)!!


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

        changePresets.setOnPreferenceClickListener {
            showChangePresetLevelsDialog()
            true
        }

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
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Failed to create encrypted preferences: security error", e)
            Toast.makeText(context, R.string.password_save_error, Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create encrypted preferences: IO error", e)
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

    private fun showChangePresetLevelsDialog() {
        val dialog = ChangePresetDialog()


        parentFragmentManager.setFragmentResultListener(
            "volumes",
            viewLifecycleOwner
        ) { _, bundle ->

            val volumes = BundleCompat.getParcelableArrayList(
                bundle,
                "volumes",
                Volume::class.java
            ) ?: emptyList()

            volumes.forEach {
                when (it.stream) {
                    AudioManager.STREAM_RING ->
                        preferenceManager.sharedPreferences?.edit {
                            putInt(
                                RING_PRESET_PREFERENCE,
                                it.value.coerceIn(it.min, it.max)
                            )
                        }

                    AudioManager.STREAM_MUSIC ->
                        preferenceManager.sharedPreferences?.edit {
                            putInt(
                                MEDIA_VOLUME_PRESET_PREFERENCE,
                                it.value.coerceIn(it.min, it.max)
                            )
                        }

                    AudioManager.STREAM_VOICE_CALL ->
                        preferenceManager.sharedPreferences?.edit {
                            putInt(CALL_VOLUME_PRESET_PREFERENCE, it.value.coerceIn(it.min, it.max))
                        }

                    AudioManager.STREAM_NOTIFICATION ->
                        preferenceManager.sharedPreferences?.edit {
                            putInt(
                                NOTIFICATION_VOLUME_PRESET_PREFERENCE,
                                it.value.coerceIn(it.min, it.max)
                            )
                        }

                    AudioManager.STREAM_ALARM ->
                        preferenceManager.sharedPreferences?.edit {
                            putInt(
                                ALARM_VOLUME_PRESET_PREFERENCE,
                                it.value.coerceIn(it.min, it.max)
                            )
                        }
                }
            }
        }
        dialog.show(parentFragmentManager, "ChangePresetDialog")

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
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Failed to save password: security error", e)
            Toast.makeText(context, R.string.password_save_error, Toast.LENGTH_SHORT).show()
            false
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save password: IO error", e)
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

class ChangePresetDialog : DialogFragment() {

    private var volumes: List<Volume> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {


        val view = layoutInflater.inflate(R.layout.dialog_presets, null)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.change_presets)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()


        dialog.setOnShowListener {
            val fragment = VolumeSliderFragment().apply {
                arguments = bundleOf(
                    "inPreferenceMode" to true
                )
            }

            childFragmentManager.beginTransaction()
                .replace(R.id.volumeSliderFragment, fragment)
                .commit()

            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                volumes = fragment.getVolumePresets()
                parentFragmentManager.setFragmentResult(
                    "volumes",
                    bundleOf("volumes" to ArrayList(volumes))
                )

                dismiss()
            }
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            val height = (resources.displayMetrics.heightPixels * 1.0).toInt()

            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height
            )
        }
    }
}