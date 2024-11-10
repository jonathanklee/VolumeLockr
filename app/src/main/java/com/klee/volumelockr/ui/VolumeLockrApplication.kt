package com.klee.volumelockr.ui

import android.app.Application
import com.google.android.material.color.DynamicColors

class VolumeLockrApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}