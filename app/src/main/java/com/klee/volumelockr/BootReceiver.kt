package com.klee.volumelockr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.klee.volumelockr.schedule.ScheduleManager
import com.klee.volumelockr.service.VolumeService

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "VolumeLockr"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "BootReceiver: 收到开机广播，读取锁定配置")
            val prefs = context.getSharedPreferences(VolumeService.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE)
            val locks = prefs.getString(VolumeService.LOCKS_KEY, "")
            val hasManualLocks = !locks.isNullOrEmpty() && locks != "{}"

            // 同时检查定时调度配置
            val scheduleManager = ScheduleManager.getInstance(context)
            val scheduleConfig = scheduleManager.loadConfig()
            val hasSchedules = scheduleConfig.scheduleEnabled && scheduleConfig.slots.isNotEmpty()

            if (hasManualLocks || hasSchedules) {
                Log.i(TAG, "BootReceiver: 发现已保存的锁定项(hasManualLocks=$hasManualLocks) 或定时调度(hasSchedules=$hasSchedules) → 启动服务")
                val serviceIntent = Intent(context, VolumeService::class.java)
                serviceIntent.putExtra(VolumeService.EXTRA_FROM_BOOT, true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                Log.i(TAG, "BootReceiver: 无已保存的锁定项且无定时调度，跳过启动")
            }
        }
    }
}