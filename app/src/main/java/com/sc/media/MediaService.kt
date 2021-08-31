package com.sc.media

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.SurfaceHolder
import com.sc.scapp.R

class MediaService : Service() {
    val mMediaPlayer = MediaPlayer()  // 播放媒体文件的对象
    val mMediaBinder = MediaBinder()  // 传给Activity，使得Activity可以与我们这个Service通信
    var mIsForeground = false  // 为true时变成前台服务，为false时停止前台服务成为一般的后台服务

    // 此类的方法都是从Activity调用的
    inner class MediaBinder : Binder() {
        // 打开媒体文件
        fun open(uri: Uri) {
            mMediaPlayer.reset()
            mMediaPlayer.setDataSource(this@MediaService, uri)
            mMediaPlayer.prepareAsync()
        }

        // 设置显示视频的surfaceHolder
        fun setDisplay(surfaceHolder: SurfaceHolder?,
                       videoSizeChangedListener: MediaPlayer.OnVideoSizeChangedListener?) {
            mMediaPlayer.setDisplay(surfaceHolder)
            mMediaPlayer.setOnVideoSizeChangedListener(videoSizeChangedListener)
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return mMediaBinder
    }

    override fun onCreate() {
        super.onCreate()

        mMediaPlayer.setOnPreparedListener {
            mMediaPlayer.start()
            updateServiceState()
        }
        mMediaPlayer.setOnCompletionListener {
            updateServiceState()
        }
        mMediaPlayer.setOnErrorListener { mp, what, extra ->
            Log.e(TAG, "MediaPlayer error! mediaPlayer=$mp, what=$what, extra=$extra")
            updateServiceState()
            true
        }
    }

    // 如果正在播放媒体文件，则变成前台服务，否则变成一般的后台服务
    private fun updateServiceState() {
        if (mMediaPlayer.isPlaying != mIsForeground) {
            mIsForeground = !mIsForeground
            if (mIsForeground) {
                startForegroundNotification()
            } else {
                stopForeground(true)
            }
        }
    }

    // 创建notification，变成前台服务
    private fun startForegroundNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("Media", "Media", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, MediaActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val notification = Notification.Builder(this, "Media")
                .setSmallIcon(R.drawable.ic_media)
                .setContentTitle("Media")
                .setContentText("Playing...")
                .setContentIntent(pendingIntent)
                .build()
        startForeground(123, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaPlayer.release()
    }

    companion object {
        private val TAG = MediaService::class.simpleName!!
    }
}