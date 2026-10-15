package com.klee.volumelockr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.klee.volumelockr.service.VolumeLockStore
import com.klee.volumelockr.service.VolumeService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        if (VolumeLockStore(context).hasLocks()) {
            VolumeService.start(context)
        }
    }
}
