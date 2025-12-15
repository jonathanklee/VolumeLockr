package com.klee.volumelockr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.klee.volumelockr.service.VolumeService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(VolumeService.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE)
            val locks = prefs.getString(VolumeService.LOCKS_KEY, "")

            if (!locks.isNullOrEmpty() && locks != "{}") {
                VolumeService.start(context)
            }
        }
    }
}
