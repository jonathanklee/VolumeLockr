package com.klee.volumelockr

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import androidx.annotation.RequiresApi

class VolumeService : Service() {

    companion object {
        const val NOTIFICATION_TITLE = "VolumeLockr"
        const val NOTIFICATION_DESCRIPTION = "Service is running in background"
        const val NOTIFICATION_CHANNEL_ID = "VolumeService"
        const val NOTIFICATION_ID = 4455

        const val VOLUME_MUSIC_SPEAKER_SETTING = "volume_music_speaker"
        const val VOLUME_MUSIC_HEADSET_SETTING = "volume_music_headset"
        const val VOLUME_MUSIC_BT_SETTING = "volume_music_bt_a2dp"
        const val VOLUME_ALARM_SETTING = "volume_alarm"
        const val VOLUME_ALARM_SPEAKER_SETTING = "volume_alarm_speaker"
        const val VOLUME_RING_SPEAKER_SETTING = "volume_ring_speaker"
        const val VOLUME_RING_EARPIECE_SETTING = "volume_ring_earpiece"
        const val VOLUME_RING_BT_SETTING = "volume_ring_bt_ad2p"
        const val VOLUME_VOICE_EARPIECE_SETTING = "volume_voice_earpiece"
        const val VOLUME_VOICE_HEADSET_SETTING = "volume_voice_headset"
        const val VOLUME_VOICE_BT_SETTING = "volume_voice_bt_a2dp"
    }

    private lateinit var mAudioManager: AudioManager
    private var mListener: (() -> Unit)? = null
    private val mHandler = Handler(Looper.getMainLooper())
    private val mBinder = LocalBinder()
    private val mVolumeLock = HashMap<Int, Int>()

    override fun onCreate() {
        super.onCreate()

        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        registerObservers()
    }

    fun registerOnVolumeChangeListener(listener: () -> Unit) {
        mListener = listener
    }

    fun addLock(stream: Int, volume: Int) {
        mVolumeLock[stream] = volume
    }

    fun removeLock(stream: Int) {
        mVolumeLock.remove(stream)
    }

    fun getLocks() : HashMap<Int, Int> {
        return mVolumeLock
    }

    private val mVolumeObserver = object : ContentObserver(mHandler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            for ((stream, volume) in mVolumeLock) {
                mAudioManager.setStreamVolume(stream, volume, 0)
            }

            mListener?.invoke()
        }
    }

    private fun fetchUri(setting: String) : Uri = Settings.System.getUriFor(setting)

    private fun registerObservers() {
        registerObserver(VOLUME_MUSIC_SPEAKER_SETTING)
        registerObserver(VOLUME_MUSIC_HEADSET_SETTING)
        registerObserver(VOLUME_MUSIC_BT_SETTING)

        registerObserver(VOLUME_ALARM_SETTING)
        registerObserver(VOLUME_ALARM_SPEAKER_SETTING)

        registerObserver(VOLUME_RING_SPEAKER_SETTING)
        registerObserver(VOLUME_RING_EARPIECE_SETTING)
        registerObserver(VOLUME_RING_BT_SETTING)

        registerObserver(VOLUME_VOICE_EARPIECE_SETTING)
        registerObserver(VOLUME_VOICE_HEADSET_SETTING)
        registerObserver(VOLUME_VOICE_BT_SETTING)
    }

    private fun registerObserver(setting: String) {
        contentResolver.registerContentObserver(fetchUri(setting), true, mVolumeObserver)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun showNotification() {

        if (mVolumeLock.size != 1) {
            return
        }

        createNotificationChannel()
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_DESCRIPTION)
            .setSmallIcon(R.drawable.ic_volumelockr_foreground)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(Build.VERSION_CODES.N)
    fun hideNotification() {
        if (mVolumeLock.size > 0) {
            return
        }

        stopForeground(NOTIFICATION_ID)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "VolumeLockrChannel", NotificationManager.IMPORTANCE_LOW)

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    inner class LocalBinder : Binder() {
        fun getService(): VolumeService = this@VolumeService
    }

    override fun onBind(p0: Intent?): IBinder {
        return mBinder
    }
}
