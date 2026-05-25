package com.klee.volumelockr.ui

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.klee.volumelockr.R
import com.klee.volumelockr.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 延迟应用 AppTask 隐藏设置（等任务记录创建完毕）
        applyRecentsHiding()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
        setupWindowInsets()
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkDoNotDisturbPermission()
        }
    }

    /**
     * 通过 ActivityManager.AppTask.setExcludeFromRecents() 动态控制最近任务可见性
     *
     * 这是"李跳跳"等应用使用的方式：
     * - 不同于 Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS（仅启动时有效，MIUI 会覆盖）
     * - 不同于 Manifest excludeFromRecents（静态，不可切换）
     * - AppTask.setExcludeFromRecents() 直接操作 AMS 的任务记录，MIUI 无法覆盖
     * - 需要延迟执行，因为 AppTask 在 onCreate 中可能尚未就绪
     */
    private fun applyRecentsHiding() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val hideRecents = prefs.getBoolean("hide_recents_enabled", false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 延迟 500ms 确保 AppTask 已创建
            handler.postDelayed({
                setExcludeFromRecents(hideRecents)
            }, 500)
        }
    }

    /**
     * 直接操作 AppTask 设置最近任务排除
     * 对 MIUI/HyperOS 有效，因为它操作的是系统级任务记录而非 Intent 标志
     */
    fun setExcludeFromRecents(exclude: Boolean) {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val tasks = am.appTasks
            if (tasks.isNotEmpty()) {
                tasks[0].setExcludeFromRecents(exclude)
            }
        } catch (e: Exception) {
            // 部分设备可能不支持，静默失败
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkDoNotDisturbPermission() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (notificationManager.isNotificationPolicyAccessGranted) {
            return
        }

        PolicyAccessDialog().show(supportFragmentManager, PolicyAccessDialog.TAG)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, 0, insets.right, 0)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container_view) as NavHostFragment

        val navController = navHostFragment.navController
        val navView: BottomNavigationView = binding.bottomNavigation!!

        val appBarConfiguration = AppBarConfiguration
            .Builder(R.id.volumeSliderFragment, R.id.scheduleFragment, R.id.settingsFragment, R.id.about_libraries)
            .build()

        navView.setupWithNavController(navController)
    }
}