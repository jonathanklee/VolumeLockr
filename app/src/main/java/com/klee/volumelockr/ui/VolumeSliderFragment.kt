package com.klee.volumelockr.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.klee.volumelockr.R
import com.klee.volumelockr.databinding.FragmentVolumeSliderBinding
import com.klee.volumelockr.service.VolumeService

class VolumeSliderFragment : Fragment() {

    private var _binding: FragmentVolumeSliderBinding? = null
    private val binding get() = _binding!!
    private var mAdapter: VolumeAdapter? = null
    private var mService: VolumeService? = null
    private var isServiceBound = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVolumeSliderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mService?.let {
            handleServiceConnected()
        } ?: Intent(context, VolumeService::class.java).also { intent ->
            context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onPause() {
        unbindServiceIfNeeded()
        super.onPause()
    }

    override fun onDestroyView() {
        unbindServiceIfNeeded()
        _binding?.recyclerView?.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupRecyclerView(service: VolumeService) {
        val spanCount = if (resources.getBoolean(R.bool.use_two_columns)) 2 else 1
        binding.recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), spanCount)
        mAdapter = VolumeAdapter(service.getVolumes(), service, requireContext())
        binding.recyclerView.adapter = mAdapter
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as VolumeService.LocalBinder
            mService = binder.getService()
            isServiceBound = true
            handleServiceConnected()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isServiceBound = false
            mService = null
            mAdapter = null
        }
    }

    private fun handleServiceConnected() {
        mService?.let {
            setupRecyclerView(it)

            mService?.registerOnVolumeChangeListener(Handler(Looper.getMainLooper())) {
                mAdapter?.update(it.getVolumes())
            }
        }
    }

    private fun unbindServiceIfNeeded() {
        mService?.unregisterOnModeChangeListener()

        if (isServiceBound) {
            context?.let {
                runCatching { it.unbindService(connection) }
            }
            isServiceBound = false
        }

        mService = null
        mAdapter = null
    }
}
