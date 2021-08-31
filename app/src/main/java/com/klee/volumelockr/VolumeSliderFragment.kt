package com.klee.volumelockr

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VolumeSliderFragment : Fragment() {

    private lateinit var mRecyclerView: RecyclerView
    private var mService: VolumeService? = null

    private val viewModel: VolumeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_volume_slider, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mRecyclerView = requireView().findViewById(R.id.recycler_view)
        mRecyclerView.layoutManager = LinearLayoutManager(context)

        initViewModel()

        Intent(context, VolumeService::class.java).also { intent ->
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
        if (item.itemId == R.id.about) {
            findNavController().navigate(R.id.action_x_to_about_libs)
        }
        return true
    }

    private fun initViewModel() {
        viewModel.getVolumes().observe(viewLifecycleOwner, Observer { volumes ->
            val adapter = VolumeAdapter(volumes, mService, requireContext())
            mRecyclerView.adapter = adapter
        })

        viewModel.loadVolumes()
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as VolumeService.LocalBinder
            mService = binder.getService()

            mService?.registerOnVolumeChangeListener(Handler(Looper.getMainLooper())) {
                viewModel.loadVolumes()
            }

            mService?.registerOnModeChangeListener {
                viewModel.loadVolumes()
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {}
    }
}