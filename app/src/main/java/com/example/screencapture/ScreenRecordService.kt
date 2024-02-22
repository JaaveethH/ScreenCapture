package com.example.screencapture

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.Locale


/**
 * Created by Jaaveeth H on 2024/2/21.
 */

@Suppress("DEPRECATION")
class ScreenRecordService : Service() {
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mScreenDensity = 0
    private var mResultCode = 0
    private var mResultData: Intent? = null


    private var isVideoSd = false

    private var isAudio = false
    private var mMediaProjection: MediaProjection? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service onCreate() is called")
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service onStartCommand() is called")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "001"
            val channelName = "myChannel"
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
            channel.lightColor = Color.BLUE
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            val notification: Notification =
                Notification.Builder(applicationContext, channelId).setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher).setCategory(
                Notification.CATEGORY_SERVICE
            ).build()
            startForeground(101, notification)
        } else {
            startForeground(101, Notification())
        }
        mResultCode = intent.getIntExtra("code", -1)
        mResultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("data", Intent::class.java)
        } else {
            intent.getParcelableExtra("data")
        }
        mScreenWidth = intent.getIntExtra("width", 720)
        mScreenHeight = intent.getIntExtra("height", 1280)
        mScreenDensity = intent.getIntExtra("density", 1)
        isVideoSd = intent.getBooleanExtra("quality", true)
        isAudio = intent.getBooleanExtra("audio", true)
        mMediaProjection = createMediaProjection()
        mMediaRecorder = createMediaRecorder()
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder?.start()
        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(this, getString(R.string.please_check_your_downloads), Toast.LENGTH_SHORT).show()
            stopSelf() // stopping service after the MAX_DURATION
        }, MAX_DURATION.toLong())
        return START_STICKY
    }

    private fun createMediaProjection(): MediaProjection? {
        Log.i(TAG, "Create MediaProjection")
        mResultData?.let {
            return (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(
                mResultCode,
                it
            )
        }

        return null
    }

    private fun createMediaRecorder(): MediaRecorder {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())
        val curDate = Date(System.currentTimeMillis())
        val curTime = formatter.format(curDate).replace(" ", "")
        var videoQuality = "HD"
        if (isVideoSd) {
            videoQuality = "SD"
        }
        Log.i(TAG, "Create MediaRecorder")
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }
        mediaRecorder.apply {
            if (isAudio) {
                setAudioSource(MediaRecorder.AudioSource.MIC)
            }
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setMaxDuration(MAX_DURATION) // max duration 30s
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .toString() + "/" + getAppName() + videoQuality + curTime + ".mp4"
            )
           setVideoSize(
                mScreenWidth,
                mScreenHeight
            ) //after setVideoSource(), setOutFormat()
            setVideoEncoder(MediaRecorder.VideoEncoder.H264) //after setOutputFormat()
            if (isAudio) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC) //after setOutputFormat()
            }
            val bitRate: Int = if (isVideoSd) {
                setVideoEncodingBitRate(mScreenWidth * mScreenHeight)
                setVideoFrameRate(30)
                mScreenWidth * mScreenHeight / 1000
            } else {
                setVideoEncodingBitRate(5 * mScreenWidth * mScreenHeight)
                setVideoFrameRate(60) //after setVideoSource(), setOutFormat()
                5 * mScreenWidth * mScreenHeight / 1000
            }
            try {
                prepare()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "createMediaRecorder: e = $e")
            } catch (e: IOException) {
                Log.e(TAG, "createMediaRecorder: e = $e")
            }
            Log.i(
                TAG,
                "Audio: " + isAudio + ", SD video: " + isVideoSd + ", BitRate: " + bitRate + "kbps"
            )
        }
        return mediaRecorder
    }

    private fun getAppName(): String? {
        val packageManager = applicationContext.packageManager
        val applicationInfo: ApplicationInfo = try {
            packageManager.getApplicationInfo(applicationContext.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            return null
        }
        return (packageManager.getApplicationLabel(applicationInfo)) as String
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        Log.i(TAG, "Create VirtualDisplay")
        return mMediaProjection?.createVirtualDisplay(
            TAG,
            mScreenWidth,
            mScreenHeight,
            mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder!!.surface,
            null,
            null
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Service onDestroy")
        stopForeground(true)

        mVirtualDisplay?.release()
        mMediaRecorder?.apply {
            setOnErrorListener(null)
            reset()
        }

        mMediaProjection?.stop()
        mMediaProjection = null
        mVirtualDisplay = null
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private val TAG = ScreenRecordService::class.java.simpleName
        private val MAX_DURATION = 30000
    }
}