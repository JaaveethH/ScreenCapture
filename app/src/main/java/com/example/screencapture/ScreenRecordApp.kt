package com.example.screencapture

import android.app.Application
import android.util.Log

/**
 * Created by Jaaveeth H on 2024/2/20.
 */
class ScreenRecordApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")
    }

    companion object {
        private val TAG = ScreenRecordApp::class.java.simpleName
    }
}