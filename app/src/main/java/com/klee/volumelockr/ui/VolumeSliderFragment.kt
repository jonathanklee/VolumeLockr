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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.klee.volumelockr.R
import com.klee.volumelockr.VolumeAdapter
import com.klee.volumelockr.databinding.FragmentVolumeSliderBinding
import com.klee.volumelockr.service.VolumeService

class VolumeSliderFragment : Fragment() {

    private lateinit var mBinding: FragmentVolumeSliderBinding
    private lateinit var mAdapter: VolumeAdapter
    private var mService: VolumeService? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        mBinding = FragmentVolumeSliderBinding.inflate(layoutInflater)
        return mBinding.root
    }

    override fun onResume() {
        super.onResume()
        mService?.let {
            onServiceConnected()
        } ?: Intent(context, VolumeService::class.java).also { intent ->
            context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
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
        when (item.itemId) {
            R.id.about -> findNavController().navigate(R.id.action_x_to_about_libs)
            R.id.options -> findNavController().navigate(R.id.action_sliders_to_settings)
        }
        return true
    }

    private fun setupRecyclerView(service: VolumeService) {
        mBinding.recyclerView.layoutManager = LinearLayoutManager(context)
        mAdapter = VolumeAdapter(service.getVolumes(), service, requireContext())
        mBinding.recyclerView.adapter = mAdapter
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as VolumeService.LocalBinder
            mService = binder.getService()
            onServiceConnected()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            mService = null
        }
    }

    private fun onServiceConnected() {
        mService?.let {
            setupRecyclerView(it)

            mService?.registerOnVolumeChangeListener(Handler(Looper.getMainLooper())) {
                mAdapter.update(it.getVolumes())
            }

            mService?.registerOnModeChangeListener {
                mAdapter.update()
            }
        }
    }
}
