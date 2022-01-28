package com.klee.volumelockr

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.klee.volumelockr.databinding.VolumeCardBinding

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
        holder.binding.seekBar.progress = volume.value
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.binding.seekBar.min = volume.min
        }
        holder.binding.seekBar.max = volume.max

        registerSeekBarCallback(holder, volume)
        registerSwitchButtonCallback(holder, volume)

        loadLockFromService(holder, volume)

        handleRingerMode(holder, volume)

        if (isPasswordProtected()) {
            holder.binding.seekBar.isEnabled = false
            holder.binding.switchButton.isEnabled = false
        }
    }

    private fun registerSeekBarCallback(holder: ViewHolder, volume: Volume) {
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(view: SeekBar?, progress: Int, fromUser: Boolean) {
                if (volume.stream != AudioManager.STREAM_NOTIFICATION || mService?.getMode() == 2) {
                    mAudioManager.setStreamVolume(volume.stream, progress, 0)
                }

                volume.value = progress
            }

            override fun onStartTrackingTouch(view: SeekBar?) {
            }

            override fun onStopTrackingTouch(view: SeekBar?) {
            }
        }
        holder.binding.seekBar.setOnSeekBarChangeListener(listener)
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
                    holder.binding.seekBar.isEnabled = false
                }
            }
        }
    }

    private fun adjustService() {
        mService?.getLocks()?.let {
            if (it.size > 0) {
                mService?.startLocking()
            } else {
                mService?.stopLocking()
            }
        }
    }

    private fun adjustNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mService?.getLocks()?.let {
                if (it.size > 0) {
                    mService?.tryShowNotification()
                } else {
                    mService?.tryHideNotification()
                }
            }
        }
    }

    private fun handleRingerMode(holder: ViewHolder, volume: Volume) {
        if (volume.stream == AudioManager.STREAM_NOTIFICATION) {
            holder.binding.seekBar.isEnabled =
                mService?.getMode() == 2 &&
                mService?.getLocks()?.containsKey(AudioManager.STREAM_NOTIFICATION) == false
        }
    }

    private fun onVolumeLocked(holder: ViewHolder, volume: Volume) {
        mService?.addLock(volume.stream, volume.value)
        adjustService()
        adjustNotification()
        holder.binding.seekBar.isEnabled = false
    }

    private fun onVolumeUnlocked(holder: ViewHolder, volume: Volume) {
        mService?.removeLock(volume.stream)
        adjustService()
        adjustNotification()
        holder.binding.seekBar.isEnabled = true
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
