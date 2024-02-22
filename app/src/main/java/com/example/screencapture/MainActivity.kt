package com.example.screencapture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Created by Jaaveeth H on 2024/2/21.
 */

class MainActivity : AppCompatActivity() {
    private var isPermissible = false
    private var isRecording = false
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mScreenDensity = 0


    private var isVideoSd = false


    private var isAudio = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO,
                PERMISSION_REQ_ID_RECORD_AUDIO
            ) && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE
            )
        ) {
            isPermissible = true
        }
        Log.d(TAG, "onCreate: isPermissible = $isPermissible")
        view
        screenBaseInfo
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        Log.d(TAG, "checkSelfPermission $permission $requestCode")
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode)
        when (requestCode) {
            PERMISSION_REQ_ID_RECORD_AUDIO -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE
                )
            } else {
                isPermissible = false
                showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO)
                finish()
            }

            PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: isPermissible = $isPermissible")
                isPermissible = true
            } else {
                isPermissible = false
                showLongToast("No permission for " + Manifest.permission.WRITE_EXTERNAL_STORAGE)
                finish()
            }

            else -> {}
        }
    }

    private fun showLongToast(msg: String?) {
        Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
    }

    private val view: Unit
        get() {
            val startTv = findViewById<View>(R.id.button_start) as TextView
            startTv.setOnClickListener {
                if (isPermissible) {
                    startScreenRecording()
                }
            }
            val stopTv = findViewById<View>(R.id.button_stop) as TextView
            stopTv.setOnClickListener { stopScreenRecording() }
            val radioGroup = findViewById<View>(R.id.radio_group) as RadioGroup
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.sd_button -> isVideoSd = true
                    R.id.hd_button -> isVideoSd = false
                    else -> {}
                }
            }
            val audioBox = findViewById<View>(R.id.audio_check_box) as CheckBox
            audioBox.setOnCheckedChangeListener { _, isChecked -> isAudio = isChecked }
        }
    @Suppress("DEPRECATION")
    private val screenBaseInfo: Unit
        get() {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                 val windowMetrics = windowManager.currentWindowMetrics
                 val bounds = windowMetrics.bounds
                 val density = windowMetrics.density
                 mScreenWidth = bounds.width()
                 mScreenHeight = bounds.height()
                 mScreenDensity = density.toInt()
            } else {
                 val displayMetrics = DisplayMetrics()
                 windowManager.defaultDisplay.getMetrics(displayMetrics)
                 mScreenWidth = displayMetrics.widthPixels
                 mScreenHeight = displayMetrics.heightPixels
                 mScreenDensity = displayMetrics.densityDpi
            }
        }

    private fun startScreenRecording() {
        val mediaProjectionManager =
            this.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent = mediaProjectionManager.createScreenCaptureIntent()
        startRecordingLauncher.launch(permissionIntent)
    }

    private val startRecordingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Permission granted, start the recording service
                val serviceIntent = Intent(this, ScreenRecordService::class.java).apply {
                    putExtra("code", result.resultCode)
                    putExtra("data", result.data)
                    putExtra("audio", isAudio)
                    putExtra("width", mScreenWidth)
                    putExtra("height", mScreenHeight)
                    putExtra("density", mScreenDensity)
                    putExtra("quality", isVideoSd)
                }
                startService(serviceIntent)
                showToast(getString(R.string.screen_recording_started))
                isRecording = true
            } else {
                showToast(getString(R.string.user_cancelled))
                isRecording = false
                // User canceled the permission request
            }
        }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun stopScreenRecording() = if (isRecording) {
        val service = Intent(this, ScreenRecordService::class.java)
        stopService(service)
        showToast(getString(R.string.please_check_your_downloads))
        isRecording = false
    } else {
        showToast(getString(R.string.kindly_start_the_recording))
    }

    override fun onPause() {
        Log.d(TAG, "onPause: ")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop: ")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            moveTaskToBack(true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val PERMISSION_REQ_ID_RECORD_AUDIO = 22
        private const val PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE =
            PERMISSION_REQ_ID_RECORD_AUDIO + 1
    }
}