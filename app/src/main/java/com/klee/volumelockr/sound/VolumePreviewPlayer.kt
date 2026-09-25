package com.klee.volumelockr.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper

class VolumePreviewPlayer(private val mContext: Context) {

    companion object {
        private const val PREVIEW_DURATION_IN_MS = 2000L

        private val STREAM_RINGTONE_TYPES = mapOf(
            AudioManager.STREAM_MUSIC to RingtoneManager.TYPE_NOTIFICATION,
            AudioManager.STREAM_VOICE_CALL to RingtoneManager.TYPE_NOTIFICATION,
            AudioManager.STREAM_RING to RingtoneManager.TYPE_RINGTONE,
            AudioManager.STREAM_NOTIFICATION to RingtoneManager.TYPE_NOTIFICATION,
            AudioManager.STREAM_ALARM to RingtoneManager.TYPE_ALARM
        )

        private val STREAM_USAGES = mapOf(
            AudioManager.STREAM_MUSIC to AudioAttributes.USAGE_MEDIA,
            AudioManager.STREAM_VOICE_CALL to AudioAttributes.USAGE_VOICE_COMMUNICATION,
            AudioManager.STREAM_RING to AudioAttributes.USAGE_NOTIFICATION_RINGTONE,
            AudioManager.STREAM_NOTIFICATION to AudioAttributes.USAGE_NOTIFICATION,
            AudioManager.STREAM_ALARM to AudioAttributes.USAGE_ALARM
        )
    }

    private val mHandler = Handler(Looper.getMainLooper())
    private var mRingtone: Ringtone? = null

    fun play(stream: Int) {
        stop()

        val ringtone = createRingtone(stream) ?: return
        mRingtone = ringtone
        runCatching { ringtone.play() }
        mHandler.postDelayed({ stop() }, PREVIEW_DURATION_IN_MS)
    }

    fun stop() {
        mHandler.removeCallbacksAndMessages(null)
        mRingtone?.let { ringtone ->
            runCatching { ringtone.stop() }
        }
        mRingtone = null
    }

    private fun createRingtone(stream: Int): Ringtone? {
        val uri = findSoundUri(stream) ?: return null
        val ringtone = RingtoneManager.getRingtone(mContext, uri) ?: return null
        ringtone.audioAttributes = buildAudioAttributes(stream)
        return ringtone
    }

    private fun findSoundUri(stream: Int): Uri? {
        val type = STREAM_RINGTONE_TYPES[stream] ?: RingtoneManager.TYPE_NOTIFICATION
        return RingtoneManager.getActualDefaultRingtoneUri(mContext, type)
            ?: RingtoneManager.getDefaultUri(type)
    }

    private fun buildAudioAttributes(stream: Int): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(STREAM_USAGES[stream] ?: AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
