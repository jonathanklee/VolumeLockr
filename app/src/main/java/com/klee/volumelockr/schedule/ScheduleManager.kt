package com.klee.volumelockr.schedule

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

/**
 * 定时调度管理器 — 负责时间段匹配、配置持久化、导入/导出
 */
class ScheduleManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ScheduleManager"
        private const val SCHEDULE_PREFS = "volumelockr_schedule"
        private const val CONFIG_KEY = "schedule_config"

        @Volatile
        private var instance: ScheduleManager? = null

        fun getInstance(context: Context): ScheduleManager {
            return instance ?: synchronized(this) {
                instance ?: ScheduleManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(SCHEDULE_PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    /** 配置缓存，1 秒过期，避免高频读取 SharedPreferences + JSON 反序列化 */
    @Volatile
    private var configCache: ScheduleConfig? = null
    @Volatile
    private var configCacheTime = 0L

    /** 加载调度配置（内部缓存 1 秒） */
    fun loadConfig(): ScheduleConfig {
        val now = System.currentTimeMillis()
        val cached = configCache
        if (cached != null && now - configCacheTime < 1000) {
            return cached
        }
        val json = prefs.getString(CONFIG_KEY, null)
        val config = if (json == null) {
            ScheduleConfig()
        } else {
            try {
                gson.fromJson(json, object : TypeToken<ScheduleConfig>() {}.type) ?: ScheduleConfig()
            } catch (e: Exception) {
                Log.e(TAG, "加载调度配置失败", e)
                ScheduleConfig()
            }
        }
        configCache = config
        configCacheTime = now
        return config
    }

    /** 保存调度配置（同步更新缓存） */
    fun saveConfig(config: ScheduleConfig) {
        configCache = config
        configCacheTime = System.currentTimeMillis()
        prefs.edit { putString(CONFIG_KEY, gson.toJson(config)) }
    }

    /** 获取当前应该生效的时间段，没有则返回 null */
    fun getActiveSlot(): TimeSlot? {
        return getActiveSlotFromConfig(loadConfig())
    }

    /** 从已加载的配置中获取当前活跃时间段，避免重复 loadConfig() */
    fun getActiveSlotFromConfig(config: ScheduleConfig): TimeSlot? {
        if (!config.scheduleEnabled) return null

        val calendar = Calendar.getInstance()
        val dayOfWeek = calendarDayToInternal(calendar.get(Calendar.DAY_OF_WEEK))
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        return config.slots
            .filter { it.enabled }
            .firstOrNull { it.matchesDay(dayOfWeek) && it.matchesTime(hour, minute) }
    }

    /** 临时解锁是否在有效期内 */
    fun isTemporarilyUnlocked(): Boolean {
        val config = loadConfig()
        return config.temporaryUnlockUntil > System.currentTimeMillis()
    }

    /** 设置临时解锁时长（毫秒） */
    fun setTemporaryUnlock(durationMs: Long) {
        val config = loadConfig()
        saveConfig(config.copy(temporaryUnlockUntil = System.currentTimeMillis() + durationMs))
    }

    /** 取消临时解锁 */
    fun cancelTemporaryUnlock() {
        val config = loadConfig()
        saveConfig(config.copy(temporaryUnlockUntil = 0L))
    }

    /** 导出为 JSON 字符串 */
    fun exportToJson(): String {
        val config = loadConfig()
        return gson.toJson(config)
    }

    /** 从 JSON 字符串导入配置，成功返回 true */
    fun importFromJson(json: String): Boolean {
        return try {
            val config = gson.fromJson(json, object : TypeToken<ScheduleConfig>() {}.type)
                ?: return false
            saveConfig(config)
            true
        } catch (e: Exception) {
            Log.e(TAG, "导入配置失败", e)
            false
        }
    }
}