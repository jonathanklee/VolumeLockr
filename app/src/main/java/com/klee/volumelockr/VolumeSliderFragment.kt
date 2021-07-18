package com.klee.volumelockr

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.onNavDestinationSelected
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
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_volume_slider, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Intent(context, VolumeService::class.java).also { intent ->
            context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        setupRecyclerView(mService)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mService?.unregisterOnModeChangeListener()
        mService?.unregisterOnVolumeChangeListener()
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.options, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.about) {
            findNavController().navigate(R.id.action_x_to_about_libs)
        }
        return true
    }

    private fun setupRecyclerView(service: VolumeService?) {
        mRecyclerView = requireView().findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = LinearLayoutManager(context)
        mAdapter = VolumeAdapter(buildVolumesFromSettings(), service, requireContext())
        mRecyclerView.adapter = mAdapter
    }

    private fun buildVolumesFromSettings() : List<Volume> {
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
                mAdapter.update(buildVolumesFromSettings())
            }

            mService?.registerOnModeChangeListener {
                mAdapter.update()
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {}
    }
}