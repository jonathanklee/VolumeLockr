package com.klee.volumelockr.ui

import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.color.DynamicColors
import com.klee.volumelockr.R
import com.klee.volumelockr.service.VolumeService

class MainActivity : AppCompatActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION = "android.permission.POST_NOTIFICATIONS"
        private const val PERMISSION_REQUEST_CODE = 25
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkDoNotDisturbPermission()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(applicationContext, NOTIFICATION_PERMISSION)
                == PackageManager.PERMISSION_DENIED) {
                requestPermissions(arrayOf(NOTIFICATION_PERMISSION), PERMISSION_REQUEST_CODE)
            }
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
}
