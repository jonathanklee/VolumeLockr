package com.klee.volumelockr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val service = Intent(context, VolumeService::class.java)
            context.startService(service)
        }
    }
}
