package com.sc.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sc.scapp.R

class MediaActivity : AppCompatActivity(), MediaPlayer.OnVideoSizeChangedListener {
    lateinit var mSurfaceContainer: FrameLayout  // SurfaceView的全屏父View
    lateinit var mSurfaceView: SurfaceView  // 用来播放视频的SurfaceView
    lateinit var mMediaBinder: MediaService.MediaBinder  // 用来与MediaService通信的Binder对象
    var mVideoRatio = 0f  // 视频的宽高比

    // 连接MediaService的对象
    val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mMediaBinder = service as MediaService.MediaBinder
            mSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    mMediaBinder.setDisplay(holder, this@MediaActivity)
                }
                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    Log.d(TAG, "Surface changed! width=$width, height=$height")
                }
                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    mMediaBinder.setDisplay(null, null)
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) { }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)

        // 隐藏SystemUI，包括导航栏、状态栏等
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        mSurfaceContainer = findViewById(R.id.surface_container)
        mSurfaceView = findViewById(R.id.surface_view)

        // 保证转屏时可以正确更新SurfaceView的尺寸
        mSurfaceView.viewTreeObserver.addOnGlobalLayoutListener { updateSurfaceSize() }

        // 连接绑定MediaService
        val bindIntent = Intent(this, MediaService::class.java)
        bindService(bindIntent, mServiceConnection, BIND_AUTO_CREATE)

        // 打开媒体文件按钮
        val openMediaFileButton = findViewById<Button>(R.id.open_media_file_button)
        openMediaFileButton.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(this, 0)
            }
        }
    }

    // 得到打开媒体文件结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri: Uri = data?.data ?: return
        mMediaBinder.open(uri)  // 要MediaService开始播放媒体文件
    }

    // 视频尺寸改变时调用
    override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int) {
        mVideoRatio = if (height == 0) 0f else width.toFloat() / height.toFloat()
        updateSurfaceSize()
    }

    // 根据视频宽高比mVideoRatio和mSurfaceContainer的尺寸更新mSurfaceView的尺寸
    private fun updateSurfaceSize() {
        if (mVideoRatio <= 0) return
        val playerRatio = mSurfaceContainer.width.toFloat() / mSurfaceContainer.height.toFloat()
        val params = mSurfaceView.layoutParams
        if (playerRatio > mVideoRatio) {
            params.width = (mSurfaceContainer.height.toFloat() * mVideoRatio).toInt()
            params.height = mSurfaceContainer.height
        } else if (playerRatio < mVideoRatio) {
            params.width = mSurfaceContainer.width
            params.height = (mSurfaceContainer.width.toFloat() / mVideoRatio).toInt()
        } else {
            params.width = mSurfaceContainer.width
            params.height = mSurfaceContainer.height
        }
        mSurfaceView.layoutParams = params
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaBinder.setDisplay(null, null)
        unbindService(mServiceConnection)
    }

    companion object {
        private val TAG = MediaActivity::class.simpleName!!

        fun actionStart(context: Context) {
            val intent = Intent(context, MediaActivity::class.java)
            context.startActivity(intent)
        }
    }
}