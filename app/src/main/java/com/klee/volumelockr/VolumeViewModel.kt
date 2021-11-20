package com.klee.volumelockr

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData

class VolumeViewModel(application: Application) : AndroidViewModel(application) {

    private val mVolumes = MutableLiveData<List<Volume>>()
    private val mContext by lazy { application.applicationContext }

    fun getVolumes() : LiveData<List<Volume>> {
        return mVolumes
    }

    fun loadVolumes() {
        val resource = mContext.resources
        mVolumes.postValue(listOf(
            Volume(resource.getString(R.string.media_title), AudioManager.STREAM_MUSIC,
                fetchVolume(AudioManager.STREAM_MUSIC), 0, fetchMaxVolume(AudioManager.STREAM_MUSIC), false),

            Volume(resource.getString(R.string.call_title), AudioManager.STREAM_VOICE_CALL,
                fetchVolume(AudioManager.STREAM_VOICE_CALL), 1, fetchMaxVolume(AudioManager.STREAM_VOICE_CALL), false),

            Volume(resource.getString(R.string.notification_title), AudioManager.STREAM_NOTIFICATION,
                fetchVolume(AudioManager.STREAM_NOTIFICATION), 0, fetchMaxVolume(AudioManager.STREAM_NOTIFICATION),
                false),

            Volume(resource.getString(R.string.alarm_title), AudioManager.STREAM_ALARM,
                fetchVolume(AudioManager.STREAM_ALARM), 1, fetchMaxVolume(AudioManager.STREAM_ALARM), false)
        ))
    }

    private fun fetchVolume(volume: Int) : Int {
        val audioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getStreamVolume(volume)
    }

    private fun fetchMaxVolume(volume: Int) : Int {
        val audioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getStreamMaxVolume(volume)
    }
}
