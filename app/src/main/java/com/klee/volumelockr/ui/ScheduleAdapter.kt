package com.klee.volumelockr.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.klee.volumelockr.databinding.ItemScheduleBinding
import com.klee.volumelockr.schedule.DayType
import com.klee.volumelockr.schedule.TimeSlot

class ScheduleAdapter(
    private val context: Context,
    private var slots: List<TimeSlot>,
    private val onToggle: (TimeSlot, Boolean) -> Unit,
    private val onClick: (TimeSlot) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ViewHolder>() {

    fun updateSlots(newSlots: List<TimeSlot>) {
        slots = newSlots
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemScheduleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val slot = slots[position]

        holder.binding.tvSlotName.text = slot.name.ifBlank { context.getString(com.klee.volumelockr.R.string.schedule_unnamed) }
        holder.binding.tvSlotTimeRange.text = "%02d:%02d - %02d:%02d".format(
            slot.startHour, slot.startMinute, slot.endHour, slot.endMinute
        )
        holder.binding.tvSlotDays.text = getDayTypeLabel(slot.dayType)

        // 显示当前是否处于活动状态
        val isActive = checkIfActive(slot)
        holder.binding.tvSlotActive.visibility = if (isActive) View.VISIBLE else View.GONE

        holder.binding.switchSlotEnabled.isChecked = slot.enabled
        holder.binding.switchSlotEnabled.setOnCheckedChangeListener(null)
        holder.binding.switchSlotEnabled.setOnCheckedChangeListener { _, checked ->
            onToggle(slot, checked)
        }

        holder.itemView.setOnClickListener { onClick(slot) }
    }

    override fun getItemCount(): Int = slots.size

    private fun getDayTypeLabel(dayType: DayType): String = when (dayType) {
        DayType.EVERYDAY -> context.getString(com.klee.volumelockr.R.string.schedule_everyday)
        DayType.WORKDAY -> context.getString(com.klee.volumelockr.R.string.schedule_workday)
        DayType.WEEKEND -> context.getString(com.klee.volumelockr.R.string.schedule_weekend)
        DayType.CUSTOM -> context.getString(com.klee.volumelockr.R.string.schedule_everyday)
    }

    private fun checkIfActive(slot: TimeSlot): Boolean {
        if (!slot.enabled) return false
        val cal = java.util.Calendar.getInstance()
        val dayOfWeek = com.klee.volumelockr.schedule.calendarDayToInternal(cal.get(java.util.Calendar.DAY_OF_WEEK))
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        return slot.matchesDay(dayOfWeek) && slot.matchesTime(hour, minute)
    }
}