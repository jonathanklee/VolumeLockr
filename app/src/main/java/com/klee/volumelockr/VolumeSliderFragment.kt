package com.klee.volumelockr

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VolumeSliderFragment : Fragment() {

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: VolumeAdapter
    private var mService: VolumeService? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_volume_slider, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Intent(context, VolumeService::class.java).also { intent ->
            context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        setupRecyclerView(mService)
    }

    private fun setupRecyclerView(service: VolumeService?) {
        mRecyclerView = requireView().findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = LinearLayoutManager(context)
        mAdapter = service?.let {
            VolumeAdapter(updateVolumesFromSettings(), it, requireContext())
        } ?: VolumeAdapter(updateVolumesFromSettings(), null, requireContext())

        mRecyclerView.adapter = mAdapter
    }

    private fun updateVolumesFromSettings() : List<Volume> {
        return listOf(
            Volume(resources.getString(R.string.media_title), AudioManager.STREAM_MUSIC,
                fetchVolume(AudioManager.STREAM_MUSIC), 0, 25, false),

            Volume(resources.getString(R.string.call_title), AudioManager.STREAM_VOICE_CALL,
                fetchVolume(AudioManager.STREAM_VOICE_CALL), 1, 7, false),

            Volume(resources.getString(R.string.notification_title), AudioManager.STREAM_NOTIFICATION,
                fetchVolume(AudioManager.STREAM_NOTIFICATION), 0,7, false),

            Volume(resources.getString(R.string.alarm_title), AudioManager.STREAM_ALARM,
                fetchVolume(AudioManager.STREAM_ALARM), 1, 7, false),
        )
    }

    private fun fetchVolume(volume: Int) : Int {
        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getStreamVolume(volume)
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as VolumeService.LocalBinder
            mService = binder.getService()
            setupRecyclerView(mService)
            mService?.registerOnVolumeChangeListener {
                mAdapter.update(updateVolumesFromSettings())
            }

        }

        override fun onServiceDisconnected(p0: ComponentName?) {}
    }
}