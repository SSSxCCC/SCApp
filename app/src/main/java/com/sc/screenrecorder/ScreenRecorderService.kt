package com.sc.screenrecorder

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.sc.scapp.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ScreenRecorderService : Service() {
    private var mStarted = false
    private lateinit var mSaveDir: File
    private lateinit var mSaveFile: File
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private lateinit var mMediaRecorder: MediaRecorder
    private lateinit var mMediaProjection: MediaProjection
    private lateinit var mVirtualDisplay: VirtualDisplay

    override fun onCreate() {
        super.onCreate()
        mSaveDir = File(externalMediaDirs[0], "ScreenRecorder")
        if (!mSaveDir.mkdirs()) Log.e(TAG, "Failed to create directory: $mSaveDir")
        mMediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (mStarted) return super.onStartCommand(intent, flags, startId)
        mStarted = true
        mSaveFile = File(mSaveDir, SimpleDateFormat("yyyyMMddHHmmss").format(Date()) + ".mp4")

        val resultCode = intent.getIntExtra("resultCode", -1)
        val resultData = intent.getParcelableExtra<Intent>("resultData")

        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val dpi = resources.displayMetrics.densityDpi

        mMediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(width, height)
            setVideoFrameRate(30)
            setVideoEncodingBitRate(10000000)
            setOutputFile(mSaveFile)
            prepare()
            start()
        }

        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, resultData!!)
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("scScreenRecorder", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.surface, null, null)

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("ScreenRecorder", "ScreenRecorder", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, ScreenRecorderActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val notification = Notification.Builder(this, "ScreenRecorder")
                .setSmallIcon(R.drawable.ic_screen_recorder)
                .setContentTitle("ScreenRecorder")
                .setContentText("Recording...")
                .setContentIntent(pendingIntent)
                .build()
        startForeground(123, notification)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaRecorder.stop()
        mMediaRecorder.release()
        mMediaProjection.stop()
        mVirtualDisplay.release()
        Toast.makeText(this, "$mSaveFile saved.", Toast.LENGTH_LONG).show()
        stopForeground(true)
    }

    companion object {
        private val TAG = ScreenRecorderService::class.simpleName!!

        fun actionStart(context: Context, resultCode: Int, resultData: Intent?) {
            val intent = Intent(context, ScreenRecorderService::class.java)
            intent.putExtra("resultCode", resultCode)
            intent.putExtra("resultData", resultData)
            context.startForegroundService(intent)
        }
    }
}