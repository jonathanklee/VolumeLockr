package com.klee.volumelockr.schedule

import java.util.UUID

/** 星期类型 */
enum class DayType {
    EVERYDAY,   // 每天
    WORKDAY,    // 周一至周五
    WEEKEND,    // 周六、周日
    CUSTOM      // 自定义
}

/** 单个时间段配置 */
data class TimeSlot(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endHour: Int = 23,
    val endMinute: Int = 59,
    val dayType: DayType = DayType.EVERYDAY,
    val customDays: Set<Int> = emptySet(),         // 0=周日, 1=周一 … 6=周六
    val volumes: Map<Int, Int> = emptyMap(), // AudioStream → 目标音量 0-100(%)
    val enabled: Boolean = true
) {
    /** 时间段的分钟表示 (startHour*60+startMinute) */
    val startMinutes: Int get() = startHour * 60 + startMinute

    /** 时间段的分钟表示 (endHour*60+endMinute) */
    val endMinutes: Int get() = endHour * 60 + endMinute

    /** 判断是否跨天 (如 22:00-06:00) */
    val crossesMidnight: Boolean get() = startMinutes > endMinutes

    /** 检查给定的星期是否匹配此时间段的星期规则 */
    fun matchesDay(dayOfWeek: Int /* 1=周日 Calendar.SUNDAY → 需转换为 0=周日 */): Boolean {
        return when (dayType) {
            DayType.EVERYDAY -> true
            DayType.WORKDAY  -> dayOfWeek in setOf(1, 2, 3, 4, 5)  // 周一~周五 (Calendar: 2-6)
            DayType.WEEKEND  -> dayOfWeek in setOf(0, 6)             // 周六日 (转换后)
            DayType.CUSTOM   -> dayOfWeek in customDays
        }
    }

    /** 检查给定的时分是否落在此时间段内 */
    fun matchesTime(hour: Int, minute: Int): Boolean {
        val nowMinutes = hour * 60 + minute
        return if (crossesMidnight) {
            nowMinutes >= startMinutes || nowMinutes < endMinutes
        } else {
            nowMinutes in startMinutes until endMinutes
        }
    }
}

/** 完整调度配置 */
data class ScheduleConfig(
    val slots: List<TimeSlot> = emptyList(),
    val temporaryUnlockUntil: Long = 0L,    // 临时解锁过期时间戳 (System.currentTimeMillis)
    val scheduleEnabled: Boolean = false     // 调度总开关
)

/** 辅助：将 Calendar.DAY_OF_WEEK (1=周日) 转换为内部表示 (0=周日) */
fun calendarDayToInternal(calendarDay: Int): Int = (calendarDay - 1) % 7