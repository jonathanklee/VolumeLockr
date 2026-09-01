package com.klee.volumelockr.service

import android.content.Context
import android.media.AudioManager
import com.klee.volumelockr.R
import com.klee.volumelockr.ui.Volume

class VolumeProvider(private val mContext: Context) {

    companion object {
        const val MIN_MUSIC_VOLUME = 0
        const val MIN_CALL_VOLUME = 1
        const val MIN_NOTIFICATION_VOLUME = 0
        const val MIN_ALARM_VOLUME = 1
    }

    private val mAudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun getVolumes(): List<Volume> {
        val resource = mContext.resources
        return listOf(
            Volume(
                resource.getString(R.string.media_title),
                AudioManager.STREAM_MUSIC,
                fetchVolume(AudioManager.STREAM_MUSIC),
                MIN_MUSIC_VOLUME,
                fetchMaxVolume(AudioManager.STREAM_MUSIC),
                false
            ),

            Volume(
                resource.getString(R.string.call_title),
                AudioManager.STREAM_VOICE_CALL,
                fetchVolume(AudioManager.STREAM_VOICE_CALL),
                MIN_CALL_VOLUME,
                fetchMaxVolume(AudioManager.STREAM_VOICE_CALL),
                false
            ),

            Volume(
                resource.getString(R.string.ring_title),
                AudioManager.STREAM_RING,
                fetchVolume(AudioManager.STREAM_RING),
                0,
                fetchMaxVolume(AudioManager.STREAM_RING),
                false
            ),

            Volume(
                resource.getString(R.string.notification_title),
                AudioManager.STREAM_NOTIFICATION,
                fetchVolume(AudioManager.STREAM_NOTIFICATION),
                MIN_NOTIFICATION_VOLUME,
                fetchMaxVolume(AudioManager.STREAM_NOTIFICATION),
                false
            ),

            Volume(
                resource.getString(R.string.alarm_title),
                AudioManager.STREAM_ALARM,
                fetchVolume(AudioManager.STREAM_ALARM),
                MIN_ALARM_VOLUME,
                fetchMaxVolume(AudioManager.STREAM_ALARM),
                false
            )
        )
    }

    private fun fetchVolume(volume: Int): Int {
        return mAudioManager.getStreamVolume(volume)
    }

    fun fetchMaxVolume(volume: Int): Int {
        return mAudioManager.getStreamMaxVolume(volume)
    }
}
