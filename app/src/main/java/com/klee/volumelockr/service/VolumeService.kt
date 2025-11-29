package com.klee.volumelockr.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.klee.volumelockr.R
import com.klee.volumelockr.ui.MainActivity
import com.klee.volumelockr.ui.SettingsFragment.Companion.ALLOW_LOWER_PREFERENCE
import com.klee.volumelockr.ui.Volume
import java.util.Timer
import java.util.TimerTask

class VolumeService : Service() {

    companion object {
        const val NOTIFICATION_TITLE = "VolumeLockr"
        const val NOTIFICATION_DESCRIPTION = "Service is running in background"
        const val NOTIFICATION_CHANNEL_ID = "VolumeService"
        const val NOTIFICATION_ID = 4455
        const val APP_SHARED_PREFERENCES = "volumelockr_shared_preferences"
        const val LOCKS_KEY = "locks_key"

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

        const val MODE_RINGER_SETTING = "mode_ringer"

        const val PERIOD_IN_MS = 25L

        fun start(context: Context) {
            val service = Intent(context, VolumeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(service)
            } else {
                context.startService(service)
            }
        }
    }

    private lateinit var mAudioManager: AudioManager
    private lateinit var mVolumeProvider: VolumeProvider
    private var mVolumeListenerHandler: Handler? = null
    private var mVolumeListener: (() -> Unit)? = null
    private var mModeListener: (() -> Unit)? = null
    private val mBinder = LocalBinder()
    private var mVolumeLock = HashMap<Int, Int>()
    private var mMode: Int = 2
    private var mTimer: Timer? = null
    private var mAllowLower = false
    private var mAllowLowerListener: (() -> Unit)? = null

    override fun onCreate() {
        super.onCreate()

        mAudioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        mVolumeProvider = VolumeProvider(this)

        loadPreferences()

        registerObservers()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tryShowNotification()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mAllowLower = sharedPreferences.getBoolean(ALLOW_LOWER_PREFERENCE, false)

        mMode = Settings.Global.getInt(contentResolver, MODE_RINGER_SETTING)

        return START_STICKY
    }

    fun getVolumes(): List<Volume> = mVolumeProvider.getVolumes()

    @Synchronized
    fun startLocking() {
        if (mTimer != null) {
            return
        }

        mTimer = Timer()
        mTimer?.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    checkVolumes()
                }
            },
            0,
            PERIOD_IN_MS
        )
    }

    @Synchronized
    fun stopLocking() {
        mTimer?.cancel()
        mTimer = null
    }

    fun registerOnVolumeChangeListener(handler: Handler, listener: () -> Unit) {
        mVolumeListenerHandler = handler
        mVolumeListener = listener
    }

    fun unregisterOnVolumeChangeListener() {
        mVolumeListener = null
    }

    fun registerOnModeChangeListener(listener: () -> Unit) {
        mModeListener = listener
    }

    fun unregisterOnModeChangeListener() {
        mModeListener = null
    }

    fun registerOnAllowLowerChangeListener(listener: () -> Unit) {
        mAllowLowerListener = listener
    }

    fun unregisterOnAllowLowerChangeListener() {
        mAllowLowerListener = null
    }

    @Synchronized
    fun addLock(stream: Int, volume: Int) {
        mVolumeLock[stream] = volume
        savePreferences()
    }

    @Synchronized
    fun removeLock(stream: Int) {
        mVolumeLock.remove(stream)
        savePreferences()
    }

    @Synchronized
    fun getLocks(): HashMap<Int, Int> {
        return mVolumeLock
    }

    fun getMode(): Int {
        return mMode
    }

    private fun savePreferences() {
        val sharedPreferences = getSharedPreferences(APP_SHARED_PREFERENCES, MODE_PRIVATE)
        sharedPreferences.edit {
            putString(LOCKS_KEY, Gson().toJson(mVolumeLock))
        }
    }

    private fun loadPreferences() {
        val sharedPreferences = getSharedPreferences(APP_SHARED_PREFERENCES, MODE_PRIVATE)
        class Token : TypeToken<HashMap<Int, Int>>()
        val value = sharedPreferences.getString(LOCKS_KEY, "")
        if (value.isNullOrBlank()) {
            return
        }

        mVolumeLock = Gson().fromJson(value, Token().type)
        startLocking()
    }

    @WorkerThread
    @Synchronized
    private fun checkVolumes() {
        for ((stream, volume) in mVolumeLock) {
            val current = mAudioManager.getStreamVolume(stream)
            if ((current > volume) || (!mAllowLower && current != volume)) {
                mAudioManager.setStreamVolume(stream, volume, 0)
                invokeVolumeListenerCallback()
            }
        }
    }

    private val mVolumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            invokeVolumeListenerCallback()
        }
    }

    private fun invokeVolumeListenerCallback() {
        mVolumeListenerHandler?.removeCallbacksAndMessages(null)
        mVolumeListenerHandler?.post {
            mVolumeListener?.invoke()
        }
    }

    private val mModeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)

            mMode = Settings.Global.getInt(contentResolver, MODE_RINGER_SETTING)

            mModeListener?.invoke()
        }
    }

    private fun fetchUri(setting: String): Uri = Settings.System.getUriFor(setting)

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

        contentResolver.registerContentObserver(
            Settings.Global.getUriFor(MODE_RINGER_SETTING),
            true,
            mModeObserver
        )
    }

    private fun registerObserver(setting: String) {
        contentResolver.registerContentObserver(fetchUri(setting), true, mVolumeObserver)
    }

    private fun unregisterObservers() {
        runCatching {
            contentResolver.unregisterContentObserver(mVolumeObserver)
        }
        runCatching {
            contentResolver.unregisterContentObserver(mModeObserver)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Synchronized
    fun tryShowNotification() {
        if (mVolumeLock.isEmpty()) {
            return
        }

        createNotificationChannel()
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_DESCRIPTION)
            .setSmallIcon(R.drawable.ic_volumelockr_foreground)
            .setContentIntent(createNotificationContentIntent())
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(Build.VERSION_CODES.N)
    @Synchronized
    fun tryHideNotification() {
        if (mVolumeLock.isNotEmpty()) {
            return
        }

        stopForeground(NOTIFICATION_ID)
    }

    private fun createNotificationContentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(this, NOTIFICATION_ID, intent, flags)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "VolumeLockr service",
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    inner class LocalBinder : Binder() {
        fun getService(): VolumeService = this@VolumeService
    }

    override fun onBind(p0: Intent?): IBinder {
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterObservers()
        stopLocking()
    }
}
