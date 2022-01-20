package com.klee.volumelockr

import android.content.Context
import android.media.AudioManager

class VolumeProvider(private val mContext: Context) {

    private val mAudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun getVolumes(): List<Volume> {
        val resource = mContext.resources
        return listOf(
            Volume(
                resource.getString(R.string.media_title),
                AudioManager.STREAM_MUSIC,
                fetchVolume(AudioManager.STREAM_MUSIC),
                0,
                fetchMaxVolume(AudioManager.STREAM_MUSIC),
                false
            ),

            Volume(
                resource.getString(R.string.call_title),
                AudioManager.STREAM_VOICE_CALL,
                fetchVolume(AudioManager.STREAM_VOICE_CALL),
                1,
                fetchMaxVolume(AudioManager.STREAM_VOICE_CALL),
                false
            ),

            Volume(
                resource.getString(R.string.notification_title),
                AudioManager.STREAM_NOTIFICATION,
                fetchVolume(AudioManager.STREAM_NOTIFICATION),
                0,
                fetchMaxVolume(AudioManager.STREAM_NOTIFICATION),
                false
            ),

            Volume(
                resource.getString(R.string.alarm_title),
                AudioManager.STREAM_ALARM,
                fetchVolume(AudioManager.STREAM_ALARM),
                1,
                fetchMaxVolume(AudioManager.STREAM_ALARM),
                false
            )
        )
    }

    private fun fetchVolume(volume: Int): Int {
        return mAudioManager.getStreamVolume(volume)
    }

    private fun fetchMaxVolume(volume: Int): Int {
        return mAudioManager.getStreamMaxVolume(volume)
    }
}
