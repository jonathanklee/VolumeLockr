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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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
        setHasOptionsMenu(true)
        _binding = FragmentVolumeSliderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleEdgeToEdgeInsets()
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

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.options, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about -> findNavController().navigate(R.id.action_x_to_about_libs)
            R.id.options -> findNavController().navigate(R.id.action_sliders_to_settings)
        }
        return true
    }

    private fun setupRecyclerView(service: VolumeService) {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        mAdapter = VolumeAdapter(service.getVolumes(), service, requireContext())
        binding.recyclerView.adapter = mAdapter
    }

    private fun handleEdgeToEdgeInsets() {
        val recyclerView = binding.recyclerView
        val startPadding = recyclerView.paddingLeft
        val topPadding = recyclerView.paddingTop
        val endPadding = recyclerView.paddingRight
        val bottomPadding = recyclerView.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            v.setPadding(
                startPadding + bars.left,
                topPadding + bars.top,
                endPadding + bars.right,
                bottomPadding + bars.bottom
            )

            WindowInsetsCompat.CONSUMED
        }
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

            mService?.registerOnModeChangeListener {
                mAdapter?.update()
            }

            mService?.registerOnAllowLowerChangeListener {
                mAdapter?.update()
            }
        }
    }

    private fun unbindServiceIfNeeded() {
        mService?.unregisterOnModeChangeListener()
        mService?.unregisterOnVolumeChangeListener()
        mService?.unregisterOnAllowLowerChangeListener()

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
