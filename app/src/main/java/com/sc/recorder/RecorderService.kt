package com.sc.recorder

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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

class RecorderService : Service() {
    private var mStarted = false
    private lateinit var mSaveDir: File
    private lateinit var mSaveFile: File
    private lateinit var mMediaRecorder: MediaRecorder
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null

    private var mMediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.i(TAG, "MediaProjection.Callback.onStop")
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            Log.i(TAG, "MediaProjection.Callback.onCapturedContentResize: width=$width, height=$height")
        }

        override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
            Log.i(TAG, "MediaProjection.Callback.onCapturedContentVisibilityChanged: isVisible=$isVisible")
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (mStarted) return super.onStartCommand(intent, flags, startId)
        mStarted = true

        mSaveDir = File(externalMediaDirs[0], "Recorder")
        if (!mSaveDir.exists() and !mSaveDir.mkdirs()) Log.e(TAG, "Failed to create directory: $mSaveDir")

        when (intent.getIntExtra("recordType", RECORD_MIC)) {
            RECORD_SCREEN -> {
                startForegroundNotification("Screen")

                mSaveFile = File(mSaveDir, SimpleDateFormat("yyyyMMddHHmmss").format(Date()) + ".mp4")

                val resultCode = intent.getIntExtra("resultCode", -1)
                val resultData = intent.getParcelableExtra<Intent>("resultData")!!

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
                    prepare()  // TODO fix crash in Android S
                    start()
                }

                val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mMediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData)
                mMediaProjection!!.registerCallback(mMediaProjectionCallback, null)
                mVirtualDisplay = mMediaProjection!!.createVirtualDisplay("scScreenRecorder", width, height, dpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.surface, null, null)
            }
            RECORD_MIC -> {
                startForegroundNotification("Mic")

                mSaveFile = File(mSaveDir, SimpleDateFormat("yyyyMMddHHmmss").format(Date()) + ".3gp")

                mMediaRecorder = MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(mSaveFile)
                    prepare()
                    start()
                }
            }
            else -> {
                stopSelf()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundNotification(recordContent: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("Recorder", "Recorder", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, RecorderActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, "Recorder")
                .setSmallIcon(R.drawable.ic_recorder)
                .setContentTitle("Recorder")
                .setContentText("Recording $recordContent...")
                .setContentIntent(pendingIntent)
                .build()
        startForeground(123, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaRecorder.stop()
        mMediaRecorder.release()
        mMediaProjection?.stop()
        mVirtualDisplay?.release()
        Toast.makeText(this, "$mSaveFile saved.", Toast.LENGTH_LONG).show()
        stopForeground(true)
    }

    companion object {
        private val TAG = RecorderService::class.simpleName!!

        private const val RECORD_SCREEN = 0
        private const val RECORD_MIC = 1

        fun actionStartScreen(context: Context, resultCode: Int, resultData: Intent?) {
            val intent = Intent(context, RecorderService::class.java)
            intent.putExtra("resultCode", resultCode)
            intent.putExtra("resultData", resultData)
            intent.putExtra("recordType", RECORD_SCREEN)
            context.startForegroundService(intent)
        }

        fun actionStartMic(context: Context) {
            val intent = Intent(context, RecorderService::class.java)
            intent.putExtra("recordType", RECORD_MIC)
            context.startForegroundService(intent)
        }
    }
}