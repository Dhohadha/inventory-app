package com.packready.shopkeeper.data.hardware

import android.app.Activity
import android.content.Context
import android.os.PowerManager
import android.view.WindowManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenWakeLockManager @Inject constructor() {

    fun keepScreenOn(activity: Activity, enable: Boolean) {
        if (enable) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
