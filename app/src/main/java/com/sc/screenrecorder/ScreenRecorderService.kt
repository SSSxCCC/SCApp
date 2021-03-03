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

    private var started = false
    private lateinit var saveDir: File
    private lateinit var saveFile: File
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var mediaProjection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay

    override fun onCreate() {
        super.onCreate()
        saveDir = File(externalMediaDirs[0], "ScreenRecorder")
        if (!saveDir.mkdirs()) Log.e(TAG, "Failed to create directory: $saveDir")
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (started) return super.onStartCommand(intent, flags, startId)
        started = true
        saveFile = File(saveDir, SimpleDateFormat("yyyyMMddHHmmss").format(Date()) + ".mp4")

        val resultCode = intent.getIntExtra("resultCode", -1)
        val resultData = intent.getParcelableExtra<Intent>("resultData")

        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val dpi = resources.displayMetrics.densityDpi

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(width, height)
            setVideoFrameRate(30)
            setVideoEncodingBitRate(10000000)
            setOutputFile(saveFile)
            prepare()
            start()
        }

        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData!!)
        virtualDisplay = mediaProjection.createVirtualDisplay("scScreenRecorder", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.surface, null, null)

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
        mediaRecorder.stop()
        mediaRecorder.release()
        mediaProjection.stop()
        virtualDisplay.release()
        Toast.makeText(this, "$saveFile saved.", Toast.LENGTH_LONG).show()
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