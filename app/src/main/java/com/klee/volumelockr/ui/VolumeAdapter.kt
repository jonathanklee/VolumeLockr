package com.klee.volumelockr.ui

import android.content.Context
import android.content.res.ColorStateList
import android.media.AudioManager
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider
import com.klee.volumelockr.R
import com.klee.volumelockr.databinding.VolumeCardBinding
import com.klee.volumelockr.service.VolumeService
import com.google.android.material.R as MaterialR
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

class VolumeAdapter(
    private var mVolumeList: List<Volume>,
    private var mService: VolumeService?,
    private var mContext: Context,
    var onLockStateChanged: (() -> Unit)? = null,
    private val mInPreferencesMode: Boolean = false
) : RecyclerView.Adapter<VolumeAdapter.ViewHolder>() {

    companion object {
        private val STREAM_ICONS = mapOf(
            AudioManager.STREAM_MUSIC to R.drawable.ic_media,
            AudioManager.STREAM_VOICE_CALL to R.drawable.ic_call,
            AudioManager.STREAM_RING to R.drawable.ic_ring,
            AudioManager.STREAM_NOTIFICATION to R.drawable.ic_bell,
            AudioManager.STREAM_ALARM to R.drawable.ic_alarm
        )

        private val STREAM_CONTAINER_COLORS = mapOf(
            AudioManager.STREAM_MUSIC to MaterialR.attr.colorPrimaryContainer,
            AudioManager.STREAM_VOICE_CALL to MaterialR.attr.colorTertiaryContainer,
            AudioManager.STREAM_RING to MaterialR.attr.colorSecondaryFixedDim,
            AudioManager.STREAM_NOTIFICATION to MaterialR.attr.colorSecondaryContainer,
            AudioManager.STREAM_ALARM to MaterialR.attr.colorErrorContainer
        )

        private val STREAM_ON_CONTAINER_COLORS = mapOf(
            AudioManager.STREAM_MUSIC to MaterialR.attr.colorOnPrimaryContainer,
            AudioManager.STREAM_VOICE_CALL to MaterialR.attr.colorOnTertiaryContainer,
            AudioManager.STREAM_RING to MaterialR.attr.colorOnSecondaryFixedVariant,
            AudioManager.STREAM_NOTIFICATION to MaterialR.attr.colorOnSecondaryContainer,
            AudioManager.STREAM_ALARM to MaterialR.attr.colorOnErrorContainer
        )

        private val RINGER_AFFECTED_STREAMS = setOf(
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION
        )
    }

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
        holder.binding.streamIcon.setImageResource(
            STREAM_ICONS[volume.stream] ?: R.drawable.ic_media
        )
        applyStreamColors(holder, volume.stream)
        if (mInPreferencesMode) {
            holder.binding.slider.value = getVolumePresetLevel(volume).toFloat()
        } else {
            holder.binding.slider.value =
                mService?.getLocks()?.get(volume.stream)?.toFloat() ?: volume.value.toFloat()
        }
        holder.binding.slider.valueFrom = volume.min.toFloat()
        holder.binding.slider.valueTo = volume.max.toFloat()
        holder.binding.volumeValue.text =
            formatVolumeValue(holder.binding.slider.value.toInt(), volume.max)

        registerSeekBarCallback(holder, volume)
        registerLockButtonCallback(holder, volume)

        loadLockFromService(holder, volume)

        if (mInPreferencesMode) {
            holder.binding.lockButton.isEnabled = false
            holder.binding.lockButton.isVisible = false

        }else {
            handleRingerMode(holder, volume)

            if (isPasswordProtected()) {
                holder.binding.slider.isEnabled = false
                holder.binding.lockButton.isEnabled = false
            }
        }
    }

    private fun applyStreamColors(holder: ViewHolder, stream: Int) {
        val containerAttr = STREAM_CONTAINER_COLORS[stream] ?: MaterialR.attr.colorPrimaryContainer
        val onContainerAttr =
            STREAM_ON_CONTAINER_COLORS[stream] ?: MaterialR.attr.colorOnPrimaryContainer
        val containerColor = MaterialColors.getColor(holder.binding.root, containerAttr)
        val onContainerColor = MaterialColors.getColor(holder.binding.root, onContainerAttr)
        holder.binding.iconContainer.backgroundTintList = ColorStateList.valueOf(containerColor)
        holder.binding.streamIcon.imageTintList = ColorStateList.valueOf(onContainerColor)
    }

    private fun formatVolumeValue(value: Int, max: Int): String = "$value / $max"

    private fun registerSeekBarCallback(holder: ViewHolder, volume: Volume) {
        holder.binding.slider.clearOnChangeListeners()
        holder.binding.slider.addOnChangeListener(
            Slider.OnChangeListener { _, value, _ ->
                val canSetVolume = !isRingerAffectedStream(volume.stream) ||
                    mService?.getMode() == AudioManager.RINGER_MODE_NORMAL
                if (canSetVolume) {
                    mAudioManager.setStreamVolume(volume.stream, value.toInt(), 0)
                }
                volume.value = value.toInt()
                holder.binding.volumeValue.text = formatVolumeValue(value.toInt(), volume.max)
            })
        holder.binding.slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                // user started dragging the slider, do nothing
            }

            override fun onStopTrackingTouch(slider: Slider) {
                // user released the slider
                if (mInPreferencesMode) {
                    mService?.setVolumePreset(volume.stream, volume.value)
                }
            }
        })
    }

    private fun registerLockButtonCallback(holder: ViewHolder, volume: Volume) {
        holder.binding.lockButton.setOnClickListener {
            val isLocked = mService?.getLocks()?.containsKey(volume.stream) == true
            if (isLocked) {
                onVolumeUnlocked(holder, volume)
            } else {
                onVolumeLocked(holder, volume)
            }
        }
    }

    private fun loadLockFromService(holder: ViewHolder, volume: Volume) {
        val isLocked = mService?.getLocks()?.containsKey(volume.stream) == true
        if (!mInPreferencesMode) {
            applyLockedState(holder, isLocked)
            holder.binding.slider.isEnabled = !isLocked
        }
    }

    private fun applyLockedState(holder: ViewHolder, isLocked: Boolean) {
        val iconRes = if (isLocked) R.drawable.ic_lock else R.drawable.ic_lock_open
        val tintAttr =
            if (isLocked) android.R.attr.colorPrimary else MaterialR.attr.colorOnSurfaceVariant
        val tintColor = MaterialColors.getColor(holder.binding.root, tintAttr)

        holder.binding.lockButton.icon = ContextCompat.getDrawable(mContext, iconRes)
        holder.binding.lockButton.iconTint = ColorStateList.valueOf(tintColor)
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
        if (isRingerAffectedStream(volume.stream)) {
            holder.binding.slider.isEnabled =
                mService?.getMode() == AudioManager.RINGER_MODE_NORMAL &&
                mService?.getLocks()?.containsKey(volume.stream) == false
        }
    }

    private fun isRingerAffectedStream(stream: Int): Boolean {
        return RINGER_AFFECTED_STREAMS.contains(stream)
    }

    private fun onVolumeLocked(holder: ViewHolder, volume: Volume) {

        mService?.let {
            it.addLock(volume.stream, volume.value, false)
            VolumeService.start(mContext)
            adjustService()
            adjustNotification()
            holder.binding.slider.isEnabled = false
            applyLockedState(holder, true)
            onLockStateChanged?.invoke()
        }
    }

    private fun onVolumeUnlocked(holder: ViewHolder, volume: Volume) {
        mService?.let {
            it.removeLock(volume.stream)
            adjustService()
            adjustNotification()
            holder.binding.slider.isEnabled = true
            applyLockedState(holder, false)
            onLockStateChanged?.invoke()
        }
    }

    private fun isPasswordProtected(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        return sharedPreferences.getBoolean(SettingsFragment.PASSWORD_PROTECTED_PREFERENCE, false)
    }

    override fun getItemCount(): Int {
        return mVolumeList.size
    }

    private fun getVolumePresetLevel(volume: Volume): Int {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        when (volume.stream) {
            AudioManager.STREAM_MUSIC -> return sharedPreferences.getInt(
                SettingsFragment.MEDIA_VOLUME_PRESET_PREFERENCE,
                volume.min
            ).coerceIn(volume.min, volume.max)

            AudioManager.STREAM_VOICE_CALL -> return sharedPreferences.getInt(
                SettingsFragment.CALL_VOLUME_PRESET_PREFERENCE,
                volume.min
            ).coerceIn(volume.min, volume.max)

            AudioManager.STREAM_NOTIFICATION -> return sharedPreferences.getInt(
                SettingsFragment.NOTIFICATION_VOLUME_PRESET_PREFERENCE,
                volume.min
            ).coerceIn(volume.min, volume.max)

            AudioManager.STREAM_ALARM -> return sharedPreferences.getInt(
                SettingsFragment.ALARM_VOLUME_PRESET_PREFERENCE,
                volume.min
            ).coerceIn(volume.min, volume.max)

            else -> return 1
        }
    }
}

@Parcelize
data class Volume(
    val name: String,
    val stream: Int,
    var value: Int,
    val min: Int,
    val max: Int,
    var locked: Boolean
) : Parcelable
