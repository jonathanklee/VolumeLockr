package com.klee.volumelockr.ui

import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.klee.volumelockr.R
import com.klee.volumelockr.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()
        setupWindowInsets()
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkDoNotDisturbPermission()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container_view) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkDoNotDisturbPermission() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (!notificationManager.isNotificationPolicyAccessGranted) {
            PolicyAccessDialog().show(supportFragmentManager, PolicyAccessDialog.TAG)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, 0)
            WindowInsetsCompat.CONSUMED
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.fragmentContainerView) { v, windowInsets ->
             val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, 0, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }
}
