package com.klee.volumelockr.ui

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.klee.volumelockr.R
import com.klee.volumelockr.databinding.*
import com.klee.volumelockr.schedule.DayType
import com.klee.volumelockr.schedule.ScheduleConfig
import com.klee.volumelockr.schedule.ScheduleManager
import com.klee.volumelockr.schedule.TimeSlot
import java.util.UUID

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!
    private lateinit var scheduleManager: ScheduleManager
    private var config: ScheduleConfig = ScheduleConfig()
    private var adapter: ScheduleAdapter? = null

    // 文件选择器（导入）
    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleImport(it.toString()) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scheduleManager = ScheduleManager.getInstance(requireContext())
        config = scheduleManager.loadConfig()

        setupMasterSwitch()
        setupRecyclerView()
        setupButtons()
        refreshUI()
    }

    override fun onDestroyView() {
        _binding?.recyclerSchedule?.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ─── 总开关 ───────────────────────────────────────────

    private fun setupMasterSwitch() {
        binding.scheduleMasterSwitch.isChecked = config.scheduleEnabled
        binding.scheduleMasterSwitch.setOnCheckedChangeListener { _, checked ->
            config = config.copy(scheduleEnabled = checked)
            scheduleManager.saveConfig(config)
            refreshUI()
        }
    }

    // ─── RecyclerView ────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = ScheduleAdapter(
            requireContext(),
            config.slots,
            onToggle = { slot, enabled ->
                updateSlot(slot.copy(enabled = enabled))
            },
            onClick = { slot ->
                showEditDialog(slot)
            }
        )
        binding.recyclerSchedule.adapter = adapter
    }

    private fun refreshUI() {
        adapter?.updateSlots(config.slots)
        val hasSlots = config.slots.isNotEmpty() && config.scheduleEnabled
        binding.recyclerSchedule.visibility = if (hasSlots) View.VISIBLE else View.GONE
        binding.tvEmptyHint.visibility = if (hasSlots) View.GONE else View.VISIBLE
        binding.fabAddSchedule.isEnabled = config.scheduleEnabled

        // 定时调度启用时确保服务运行
        if (hasSlots) {
            com.klee.volumelockr.service.VolumeService.start(requireContext())
        }

        // 临时解锁状态
        val isTempUnlocked = scheduleManager.isTemporarilyUnlocked()
        binding.cardTemporaryUnlock.visibility = if (isTempUnlocked) View.VISIBLE else View.GONE
        if (isTempUnlocked) {
            val remaining = (config.temporaryUnlockUntil - System.currentTimeMillis()) / 60000
            binding.tvTempUnlockText.text = getString(R.string.schedule_temp_unlock_remaining, remaining)
        }
    }

    // ─── 按钮 ────────────────────────────────────────────

    private fun setupButtons() {
        binding.fabAddSchedule.setOnClickListener {
            val newSlot = TimeSlot(id = UUID.randomUUID().toString())
            showEditDialog(newSlot)
        }

        binding.btnCancelTempUnlock.setOnClickListener {
            scheduleManager.cancelTemporaryUnlock()
            config = scheduleManager.loadConfig()
            refreshUI()
        }

        binding.btnExport.setOnClickListener { handleExport() }
        binding.btnImport.setOnClickListener { importFileLauncher.launch("application/json") }
    }

    // ─── 编辑对话框 ──────────────────────────────────────

    private fun showEditDialog(slot: TimeSlot) {
        val dialogBinding = DialogTimeSlotBinding.inflate(LayoutInflater.from(requireContext()))
        val isNewSlot = slot.name.isEmpty()

        var workingSlot = slot
        setupSlotDialogBindings(dialogBinding, workingSlot)

        // 音量流 — 需要在 collectVolumesFromDialog 之前填充
        populateVolumeStreams(dialogBinding, workingSlot)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isNewSlot) R.string.schedule_add_slot else R.string.schedule_edit_slot)
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .apply {
                if (!isNewSlot) {
                    setNeutralButton(R.string.delete) { _, _ -> deleteSlot(workingSlot) }
                }
            }
            .setPositiveButton(android.R.string.ok, null) // 在 setOnShowListener 中拦截
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                // 从对话框收集所有数据
                val volumes = collectVolumesFromDialog(dialogBinding)
                workingSlot = collectSlotFromDialog(dialogBinding, workingSlot)
                workingSlot = workingSlot.copy(volumes = volumes)

                if (workingSlot.name.isBlank()) {
                    Toast.makeText(requireContext(), R.string.schedule_name_required, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (workingSlot.volumes.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.schedule_volume_required, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                saveSlot(workingSlot, isNewSlot)
                dialog.dismiss()
            }
        }

        // 时间选择按钮
        dialogBinding.btnStartTime.setOnClickListener { showTimePicker(dialogBinding.btnStartTime) }
        dialogBinding.btnEndTime.setOnClickListener { showTimePicker(dialogBinding.btnEndTime) }

        // 删除按钮（内联）
        dialogBinding.btnDeleteSlot.visibility = if (isNewSlot) View.GONE else View.VISIBLE
        dialogBinding.btnDeleteSlot.setOnClickListener {
            dialog.dismiss()
            deleteSlot(workingSlot)
        }

        dialog.show()
    }

    /** 从对话框中提取所有滑块的音量值 */
    private fun collectVolumesFromDialog(db: DialogTimeSlotBinding): MutableMap<Int, Int> {
        val volumes = mutableMapOf<Int, Int>()
        val container = db.volumeStreamsContainer
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i) as? LinearLayout ?: continue
            val slider = row.getChildAt(1) as? Slider ?: continue
            val streamType = slider.tag as? Int ?: continue
            val value = slider.value.toInt()
            if (value >= 0) volumes[streamType] = value
        }
        return volumes
    }

    private fun setupSlotDialogBindings(db: DialogTimeSlotBinding, slot: TimeSlot) {
        db.etSlotName.setText(slot.name)
        db.switchSlotEnabled.isChecked = slot.enabled
        db.btnStartTime.text = "%02d:%02d".format(slot.startHour, slot.startMinute)
        db.btnEndTime.text = "%02d:%02d".format(slot.endHour, slot.endMinute)

        // 星期选择
        when (slot.dayType) {
            DayType.EVERYDAY -> db.chipEveryday.isChecked = true
            DayType.WORKDAY -> db.chipWorkday.isChecked = true
            DayType.WEEKEND -> db.chipWeekend.isChecked = true
            DayType.CUSTOM -> {
                // 无对应 CUSTOM chip，清除选中并允许无选择状态，收集时保留原类型
                db.chipGroupDayType.clearCheck()
                db.chipGroupDayType.isSelectionRequired = false
            }
        }
    }

    private fun collectSlotFromDialog(db: DialogTimeSlotBinding, slot: TimeSlot): TimeSlot {
        val dayType = if (db.chipGroupDayType.checkedChipId == View.NO_ID) {
            slot.dayType  // 无芯片选中时保留原始类型（如 CUSTOM）
        } else when {
            db.chipEveryday.isChecked -> DayType.EVERYDAY
            db.chipWorkday.isChecked -> DayType.WORKDAY
            db.chipWeekend.isChecked -> DayType.WEEKEND
            else -> DayType.EVERYDAY
        }

        val startTime = db.btnStartTime.text.toString().split(":").map { it.toInt() }
        val endTime = db.btnEndTime.text.toString().split(":").map { it.toInt() }

        return slot.copy(
            name = db.etSlotName.text.toString().trim(),
            startHour = startTime[0],
            startMinute = startTime[1],
            endHour = endTime[0],
            endMinute = endTime[1],
            dayType = dayType,
            customDays = if (dayType == DayType.CUSTOM) slot.customDays else emptySet(),
            enabled = db.switchSlotEnabled.isChecked
        )
    }

    private fun showTimePicker(button: View) {
        val parts = (button as? com.google.android.material.button.MaterialButton)
            ?.text?.toString()?.split(":")?.map { it.toInt() } ?: listOf(8, 0)
        val picker = TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                (button as? com.google.android.material.button.MaterialButton)?.text = "%02d:%02d".format(hour, minute)
            },
            parts[0], parts[1],
            DateFormat.is24HourFormat(requireContext())
        )
        picker.show()
    }

    private fun populateVolumeStreams(db: DialogTimeSlotBinding, slot: TimeSlot) {
        val container = db.volumeStreamsContainer
        container.removeAllViews()
        val streams = getAudioStreams()
        for (stream in streams) {
            val row = createVolumeStreamRow(stream, slot.volumes[stream.stream] ?: stream.value)
            container.addView(row)
        }
    }

    private fun getAudioStreams(): List<VolumeAudioStream> {
        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return listOf(
            VolumeAudioStream(
                AudioManager.STREAM_MUSIC,
                getString(R.string.media_title),
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            ),
            VolumeAudioStream(
                AudioManager.STREAM_ALARM,
                getString(R.string.alarm_title),
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            ),
            VolumeAudioStream(
                AudioManager.STREAM_RING,
                getString(R.string.notification_title),
                audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),
                audioManager.getStreamVolume(AudioManager.STREAM_RING)
            ),
            VolumeAudioStream(
                AudioManager.STREAM_VOICE_CALL,
                getString(R.string.call_title),
                audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
            )
        )
    }

    private fun createVolumeStreamRow(stream: VolumeAudioStream, currentValue: Int): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 4, 0, 4)
        }

        val label = android.widget.TextView(requireContext()).apply {
            text = stream.name
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
        }

        val slider = Slider(requireContext()).apply {
            valueFrom = 0f
            valueTo = stream.max.toFloat()
            value = currentValue.toFloat()
            stepSize = 1f
            tag = stream.stream // store stream type in tag
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 6f)
        }

        val valueText = android.widget.TextView(requireContext()).apply {
            text = "${currentValue}/${stream.max}"
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        }

        slider.addOnChangeListener { _, value, _ ->
            valueText.text = "${value.toInt()}/${stream.max}"
        }

        row.addView(label)
        row.addView(slider)
        row.addView(valueText)
        return row
    }

    // ─── CRUD ────────────────────────────────────────────

    private fun saveSlot(slot: TimeSlot, isNew: Boolean) {
        // 从对话框 UI 重新收集音量值（因为弹窗还在显示）
        // 这里用 slot 对象传入，实际在 dialog positive 按钮中处理
        val newSlots = if (isNew) {
            config.slots + slot
        } else {
            config.slots.map { if (it.id == slot.id) slot else it }
        }
        config = config.copy(slots = newSlots)
        scheduleManager.saveConfig(config)
        refreshUI()
    }

    private fun updateSlot(slot: TimeSlot) {
        val newSlots = config.slots.map { if (it.id == slot.id) slot else it }
        config = config.copy(slots = newSlots)
        scheduleManager.saveConfig(config)
    }

    private fun deleteSlot(slot: TimeSlot) {
        config = config.copy(slots = config.slots.filter { it.id != slot.id })
        scheduleManager.saveConfig(config)
        refreshUI()
    }

    // ─── 导入/导出 ───────────────────────────────────────

    private fun handleExport() {
        exportFileLauncher.launch("VolumeLockr_Schedules.json")
    }

    private val exportFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                val json = scheduleManager.exportToJson()
                requireContext().contentResolver.openOutputStream(it)?.use { out ->
                    out.write(json.toByteArray())
                }
                Toast.makeText(requireContext(), R.string.schedule_export_success, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.schedule_export_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleImport(uriString: String) {
        try {
            val uri = android.net.Uri.parse(uriString)
            val json = requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            if (json != null && scheduleManager.importFromJson(json)) {
                config = scheduleManager.loadConfig()
                refreshUI()
                binding.scheduleMasterSwitch.isChecked = config.scheduleEnabled
                Toast.makeText(requireContext(), R.string.schedule_import_success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), R.string.schedule_import_failed, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), R.string.schedule_import_failed, Toast.LENGTH_SHORT).show()
        }
    }
}

/** 音频流简要数据 */
data class VolumeAudioStream(
    val stream: Int,
    val name: String,
    val max: Int,
    val value: Int
)