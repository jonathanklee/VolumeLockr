package com.klee.volumelockr.ui

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import com.klee.volumelockr.databinding.VolumeCardBinding
import com.klee.volumelockr.service.VolumeService

class VolumeAdapter(
    private var mVolumeList: List<Volume>,
    private var mService: VolumeService?,
    private var mContext: Context
) :
    RecyclerView.Adapter<VolumeAdapter.ViewHolder>() {

    private var mAudioManager: AudioManager =
        mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @MainThread
    fun update(volumes: List<Volume>) {
        mVolumeList = volumes
        update()
    }

    @MainThread
    fun update() {
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: VolumeCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = VolumeCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val volume = mVolumeList[position]
        holder.binding.mediaTextView.text = volume.name
        holder.binding.slider.value = mService?.getLocks()?.get(volume.stream)?.toFloat() ?: volume.value.toFloat()
        holder.binding.slider.valueFrom = volume.min.toFloat()
        holder.binding.slider.valueTo = volume.max.toFloat()

        registerSeekBarCallback(holder, volume)
        registerSwitchButtonCallback(holder, volume)

        loadLockFromService(holder, volume)

        handleRingerMode(holder, volume)

        if (isPasswordProtected()) {
            holder.binding.slider.isEnabled = false
            holder.binding.switchButton.isEnabled = false
        }
    }

    private fun registerSeekBarCallback(holder: ViewHolder, volume: Volume) {
        holder.binding.slider.clearOnChangeListeners()
        val listener =
            Slider.OnChangeListener { _, value, _ ->
                if (volume.stream != AudioManager.STREAM_NOTIFICATION || mService?.getMode() == 2) {
                    mAudioManager.setStreamVolume(volume.stream, value.toInt(), 0)
                }

                volume.value = value.toInt()
            }
        holder.binding.slider.addOnChangeListener(listener)
    }

    private fun registerSwitchButtonCallback(holder: ViewHolder, volume: Volume) {
        holder.binding.switchButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                onVolumeLocked(holder, volume)
            } else {
                onVolumeUnlocked(holder, volume)
            }
        }
    }

    private fun loadLockFromService(holder: ViewHolder, volume: Volume) {
        val locks = mService?.getLocks()?.keys
        locks?.let {
            for (key in it) {
                if (volume.stream == key) {
                    holder.binding.switchButton.isChecked = true
                    holder.binding.slider.isEnabled = false
                }
            }
        }
    }

    private fun adjustService() {
        mService?.getLocks()?.let {
            if (it.isNotEmpty()) {
                mService?.startLocking()
            } else {
                mService?.stopLocking()
            }
        }
    }

    private fun adjustNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mService?.getLocks()?.let {
                if (it.isNotEmpty()) {
                    mService?.tryShowNotification()
                } else {
                    mService?.tryHideNotification()
                }
            }
        }
    }

    private fun handleRingerMode(holder: ViewHolder, volume: Volume) {
        if (volume.stream == AudioManager.STREAM_NOTIFICATION) {
            holder.binding.slider.isEnabled =
                mService?.getMode() == 2 &&
                mService?.getLocks()?.containsKey(AudioManager.STREAM_NOTIFICATION) == false
        }
    }

    private fun onVolumeLocked(holder: ViewHolder, volume: Volume) {
        mService?.let {
            it.addLock(volume.stream, volume.value)
            adjustService()
            adjustNotification()
            holder.binding.slider.isEnabled = false
        }
    }

    private fun onVolumeUnlocked(holder: ViewHolder, volume: Volume) {
        mService?.let {
            it.removeLock(volume.stream)
            adjustService()
            adjustNotification()
            holder.binding.slider.isEnabled = true
        }
    }

    private fun isPasswordProtected(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        return sharedPreferences.getBoolean(SettingsFragment.PASSWORD_PROTECTED_PREFERENCE, false)
    }

    override fun getItemCount(): Int {
        return mVolumeList.size
    }
}

data class Volume(
    val name: String,
    val stream: Int,
    var value: Int,
    val min: Int,
    val max: Int,
    var locked: Boolean
)
