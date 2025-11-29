package com.klee.volumelockr.ui

import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.klee.volumelockr.R
import com.klee.volumelockr.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handleEdgeToEdgeInsets()
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkDoNotDisturbPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkDoNotDisturbPermission() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            PolicyAccessDialog().show(supportFragmentManager, PolicyAccessDialog.TAG)
        }
    }

    class PolicyAccessDialog : DialogFragment() {
        companion object {
            const val TAG = "PolicyAccessDialog"
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.dialog_policy_access_title))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.dialog_allow)) { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                }
                .create()
    }

    private fun handleEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
            )

            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)

            WindowInsetsCompat.CONSUMED
        }
    }


}
