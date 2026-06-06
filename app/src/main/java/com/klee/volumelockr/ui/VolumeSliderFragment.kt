package com.klee.volumelockr.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
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
        clearSubtitle()
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
        mAdapter = VolumeAdapter(service.getVolumes(), service, requireContext()).also { adapter ->
            adapter.onLockStateChanged = { updateSubtitle() }
        }
        binding.recyclerView.adapter = mAdapter
    }

    private fun setupQuickActions() {
        binding.lockAllChip.setOnClickListener { lockAll() }
        binding.unlockAllChip.setOnClickListener { unlockAll() }
        updateQuickActionState()
    }

    private fun lockAll() {
        val service = mService ?: return
        service.getVolumes().forEach { volume ->
            service.addLock(volume.stream, volume.value)
        }
        VolumeService.start(requireContext())
        service.startLocking()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            service.tryShowNotification()
        }
        mAdapter?.update(service.getVolumes())
        updateSubtitle()
    }

    private fun unlockAll() {
        val service = mService ?: return
        service.getVolumes().forEach { volume ->
            service.removeLock(volume.stream)
        }
        service.stopLocking()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            service.tryHideNotification()
        }
        mAdapter?.update(service.getVolumes())
        updateSubtitle()
    }

    private fun updateQuickActionState() {
        val isProtected = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean(SettingsFragment.PASSWORD_PROTECTED_PREFERENCE, false)
        binding.lockAllChip.isEnabled = !isProtected
        binding.unlockAllChip.isEnabled = !isProtected
    }

    private fun updateSubtitle() {
        if (!isAdded) {
            return
        }
        val service = mService ?: return
        val lockedCount = service.getLocks().size
        val totalCount = service.getVolumes().size
        val subtitle = resources.getString(R.string.locked_subtitle, lockedCount, totalCount)
        (requireActivity() as AppCompatActivity).supportActionBar?.subtitle = subtitle
    }

    private fun clearSubtitle() {
        (activity as? AppCompatActivity)?.supportActionBar?.subtitle = null
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
            setupQuickActions()
            updateSubtitle()

            mService?.registerOnVolumeChangeListener(Handler(Looper.getMainLooper())) {
                mAdapter?.update(it.getVolumes())
                updateSubtitle()
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
