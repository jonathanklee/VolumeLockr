package com.klee.volumelockr

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView

class VolumeAdapter(
    private var mVolumeList: List<Volume>,
    private var mService: VolumeService?,
    mContext: Context)
    : RecyclerView.Adapter<VolumeAdapter.ViewHolder>() {

    private var mAudioManager: AudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun update(volumes: List<Volume>) {
        mVolumeList = volumes
        notifyDataSetChanged()
    }

    fun update() {
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.media_text_view)
        val seekBar: SeekBar = view.findViewById(R.id.seek_bar)
        val switchButton: SwitchCompat = view.findViewById(R.id.switch1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.volume_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val volume = mVolumeList[position]
        holder.textView.text = volume.name
        holder.seekBar.progress = volume.value
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.seekBar.min = volume.min
        }
        holder.seekBar.max = volume.max

        registerSeekBarCallback(holder, volume)
        registerSwitchButtonCallback(holder, volume)

        loadLockFromService(holder, volume)

        handleRingerMode(holder, volume)
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
        holder.seekBar.setOnSeekBarChangeListener(listener)
    }

    private fun registerSwitchButtonCallback(holder: ViewHolder, volume: Volume) {
        holder.switchButton.setOnCheckedChangeListener { _, isChecked ->
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
                    holder.switchButton.isChecked = true
                    holder.seekBar.isEnabled = false
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
                    mService?.showNotification()
                } else {
                    mService?.hideNotification()
                }
            }
        }
    }

    private fun handleRingerMode(holder: ViewHolder, volume: Volume) {
        if (volume.stream == AudioManager.STREAM_NOTIFICATION) {
            if (mService?.getMode() != 2) {
                holder.switchButton.isEnabled = false
                holder.seekBar.isEnabled = false
            } else {
                holder.switchButton.isEnabled = true
                holder.seekBar.isEnabled = true
            }
        }
    }

    private fun onVolumeLocked(holder: ViewHolder, volume: Volume) {
        mService?.addLock(volume.stream, volume.value)
        adjustService()
        adjustNotification()
        holder.seekBar.isEnabled = false
    }

    private fun onVolumeUnlocked(holder: ViewHolder, volume: Volume) {
        mService?.removeLock(volume.stream)
        adjustService()
        adjustNotification()
        holder.seekBar.isEnabled = true
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
    var locked: Boolean)
