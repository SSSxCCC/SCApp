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
import java.util.*

class MediaService : Service() {
    val mMediaPlayer = MediaPlayer()  // 播放媒体文件的对象
    val mMediaBinder = MediaBinder()  // 传给Activity，使得Activity可以与我们这个Service通信
    var mIsForeground = false  // 为true时变成前台服务，为false时停止前台服务成为一般的后台服务
    var mMediaListener: MediaListener? = null  // mMediaPlayer的相关回调
    var mTimer: Timer? = null  // 用来定时调用mMediaListener.onProgress回调方法的定时器
    var mTimerTask = object : TimerTask() {  // 定时调用mMediaListener.onProgress回调方法的任务
        override fun run() {
            mMediaListener?.onProgress(mMediaPlayer.currentPosition, mMediaPlayer.duration)
        }
    }

    // 此类的方法都是从Activity调用的
    inner class MediaBinder : Binder() {
        // 设置显示视频的surfaceHolder
        fun setDisplay(surfaceHolder: SurfaceHolder?) {
            mMediaPlayer.setDisplay(surfaceHolder)
        }

        // 设置一个额外的媒体播放完毕时回调
        fun setMediaListener(mediaListener: MediaListener?) {
            mMediaListener = mediaListener
            mMediaPlayer.setOnVideoSizeChangedListener(mMediaListener)
            if (mMediaListener == null) {
                mTimer?.cancel()
                mTimer = null
            } else if (mTimer == null) {
                mTimer = Timer().apply { schedule(mTimerTask, 0, 100) }
            }
        }

        // 打开媒体文件
        fun open(uri: Uri) {
            mMediaPlayer.reset()
            mMediaPlayer.setDataSource(this@MediaService, uri)
            mMediaPlayer.prepareAsync()
        }

        // 继续或暂停播放媒体，返回true代表继续播放了，返回false代表暂停播放了
        fun playOrPause(): Boolean {
            if (mMediaPlayer.isPlaying) mMediaPlayer.pause()
            else mMediaPlayer.start()
            updateServiceState()
            return mMediaPlayer.isPlaying
        }

        // 停止播放媒体
        fun stop() {
            mMediaPlayer.stop()
            updateServiceState()
        }

        // 调整播放进度
        fun seekTo(millisecond: Int) {
            mMediaPlayer.seekTo(millisecond)
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
            mMediaListener?.onCompletion(it)
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
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
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