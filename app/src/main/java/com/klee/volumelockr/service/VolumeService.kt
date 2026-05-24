package com.klee.volumelockr.service

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
import android.util.Log
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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class VolumeService : Service() {

    companion object {
        private const val TAG = "VolumeLockr"

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

        /** 复用 Gson 实例，避免每次序列化都重建反射缓存 */
        private val gson = Gson()

        /** 轮询间隔 500ms — 2次/秒，开机时大幅降低系统负载 */
        private const val POLL_INTERVAL_MS = 500L

        /** 开机延迟首次轮询时间 — 60 秒给 MIUI 充分初始化音频 HAL 和所有系统服务 */
        private const val BOOT_DELAY_MS = 60000L

        /** Intent extra: 标记服务由开机广播触发 */
        const val EXTRA_FROM_BOOT = "from_boot"

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
    private val mCheckExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var mPollingFuture: ScheduledFuture<*>? = null
    private var mAllowLower = false
    private var mAllowLowerListener: (() -> Unit)? = null

    /** 标记前台服务启动是否因系统限制而失败（如开机时） */
    private var mForegroundFailed = false

    /** 轮询计数器，用于限制 logcat 输出频率 */
    private var mCheckCount = 0

    override fun onCreate() {
        super.onCreate()

        mAudioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        mVolumeProvider = VolumeProvider(this)

        loadPreferences()
        Log.i(TAG, "onCreate: 服务创建完成, locks=${mVolumeLock.size}, SDK=${Build.VERSION.SDK_INT}")

        registerObservers()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tryShowNotification()
        }

        if (mVolumeLock.isEmpty()) {
            Log.i(TAG, "onCreate: 无锁定项 → 停止服务")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(Service.STOP_FOREGROUND_REMOVE)
            }
            stopSelf()
        }
        // 不在 onCreate 中启动轮询：
        // - 开机场景：onStartCommand 会在音频 HAL 就绪后延迟启动
        // - 绑定场景：handleServiceConnected 会在 UI 初始化后启动
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mAllowLower = sharedPreferences.getBoolean(ALLOW_LOWER_PREFERENCE, false)

        mMode = Settings.Global.getInt(contentResolver, MODE_RINGER_SETTING)

        val fromBoot = intent?.getBooleanExtra(EXTRA_FROM_BOOT, false) == true
        Log.i(TAG, "onStartCommand: fromBoot=$fromBoot, locks=${mVolumeLock.size}, foregroundFailed=$mForegroundFailed")

        // 有锁定项时启动轮询，确保 onStartCommand 触发后锁定立即生效
        if (mVolumeLock.isNotEmpty()) {
            val initialDelay = if (fromBoot) BOOT_DELAY_MS else 0L
            // 开机场景：延迟 15s 启动，避开音频 HAL 初始化竞争
            // 正常场景：立即启动（包括用户从前台调用 VolumeService.start()）
            startLocking(initialDelay)
        }

        val result = if (mForegroundFailed) START_NOT_STICKY else START_STICKY
        Log.i(TAG, "onStartCommand: 返回 ${if (mForegroundFailed) "START_NOT_STICKY" else "START_STICKY"}")
        return result
    }

    fun getVolumes(): List<Volume> = mVolumeProvider.getVolumes()

    /**
     * 启动音量锁定：开启 100ms 间隔轮询。
     * @param initialDelayMs 首次轮询延迟，0 = 立即，>0 = 延迟毫秒数（如开机场景）
     */
    @Synchronized
    fun startLocking(initialDelayMs: Long = 0) {
        if (mPollingFuture != null) {
            Log.d(TAG, "startLocking: 已在轮询中，跳过 (callDelay=${initialDelayMs}ms)")
            return
        }
        mCheckCount = 0
        Log.i(TAG, "startLocking: 启动轮询, initialDelay=${initialDelayMs}ms, interval=${POLL_INTERVAL_MS}ms, locks=${mVolumeLock.size}")
        mPollingFuture = mCheckExecutor.scheduleWithFixedDelay(
            { checkVolumes() },
            initialDelayMs,
            POLL_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        )
    }

    @Synchronized
    fun stopLocking() {
        mPollingFuture?.cancel(false)
        mPollingFuture = null
    }

    fun registerOnVolumeChangeListener(handler: Handler, listener: () -> Unit) {
        mVolumeListenerHandler = handler
        mVolumeListener = listener
    }

    fun unregisterOnVolumeChangeListener() {
        mVolumeListener = null
    }

    fun unregisterOnModeChangeListener() {
        mModeListener = null
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
            putString(LOCKS_KEY, gson.toJson(mVolumeLock))
        }
    }

    private fun loadPreferences() {
        val sharedPreferences = getSharedPreferences(APP_SHARED_PREFERENCES, MODE_PRIVATE)
        class Token : TypeToken<HashMap<Int, Int>>()
        val value = sharedPreferences.getString(LOCKS_KEY, "")
        if (value.isNullOrBlank()) {
            return
        }

        mVolumeLock = gson.fromJson(value, Token().type)
    }

    @WorkerThread
    @Synchronized
    private fun checkVolumes() {
        mCheckCount++
        val shouldLog = mCheckCount <= 5 || mCheckCount % 100 == 0  // 前5次 + 每100次输出一次
        try {
            var corrections = 0
            for ((stream, volume) in mVolumeLock) {
                val current = mAudioManager.getStreamVolume(stream)
                if ((current > volume) || (!mAllowLower && current != volume)) {
                    mAudioManager.setStreamVolume(stream, volume, 0)
                    corrections++
                    invokeVolumeListenerCallback()
                }
            }
            if (shouldLog) {
                Log.d(TAG, "checkVolumes #$mCheckCount: corrections=$corrections, locked=${mVolumeLock.size}, allowLower=$mAllowLower")
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkVolumes #$mCheckCount 异常: ${e.javaClass.simpleName} - ${e.message}")
            // 捕获所有异常，防止 ScheduledExecutorService 静默中断后续调度
            // 在 MIUI 等设备上后台访问 AudioManager 可能抛 SecurityException
        }
    }

    private val mVolumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            // 仅通知 UI 更新，不触发 checkVolumes()。
            // 轮询循环 100ms 已足够快速响应（<0.1s 延迟，人无法感知），
            // ContentObserver 触发检查会在开机初期引发 12 次音频 HAL 争抢导致卡顿。
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
        createNotificationChannel()
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_DESCRIPTION)
            .setSmallIcon(R.drawable.ic_volumelockr_foreground)
            .setContentIntent(createNotificationContentIntent())
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
            Log.i(TAG, "tryShowNotification: 前台服务启动成功")
        } catch (error: IllegalStateException) {
            if (!isForegroundStartRestriction(error)) {
                throw error
            }
            Log.w(TAG, "tryShowNotification: 系统限制 → 前台服务启动失败，降级为非前台运行")
            // Android 12+ 开机时不允许后台应用启动前台服务。
            // 不要调用 stopSelf()，否则配合 onStartCommand 的 START_STICKY
            // 会形成"停止→重建→再失败→再停止"的死循环，导致开机卡顿。
            // 改为标记失败后继续运行为非前台服务。
            mForegroundFailed = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @Synchronized
    fun tryHideNotification() {
        if (mVolumeLock.isNotEmpty()) {
            return
        }

        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
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

    private fun isForegroundStartRestriction(error: IllegalStateException): Boolean {
        return error.javaClass.name == "android.app.ForegroundServiceStartNotAllowedException"
    }

    inner class LocalBinder : Binder() {
        fun getService(): VolumeService = this@VolumeService
    }

    override fun onBind(p0: Intent?): IBinder {
        return mBinder
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: 服务销毁, checkCount=$mCheckCount")
        super.onDestroy()
        stopLocking()
        mCheckExecutor.shutdownNow()
        unregisterObservers()
    }
}
