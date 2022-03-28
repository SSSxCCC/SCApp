package com.sc.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.slider.Slider
import com.sc.scapp.R

class MediaActivity : AppCompatActivity() {
    lateinit var mSurfaceContainer: FrameLayout  // SurfaceView的全屏父View
    lateinit var mSurfaceView: SurfaceView  // 用来播放视频的SurfaceView
    lateinit var mMediaControlView: View  // 媒体播放工具栏
    lateinit var mPlayPauseButton: ImageButton  // 控制媒体播放暂停的按钮
    lateinit var mSlider: Slider  // 媒体播放进度条

    lateinit var mMediaBinder: MediaService.MediaBinder  // 用来与MediaService通信的Binder对象

    var mVideoRatio = 0f  // 视频的宽高比
    var mSliderTrackingTouch = false  // mSlider是否正在被拖

    val mMediaListener = object : MediaListener {
        // 视频尺寸改变时调用
        override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int) {
            mVideoRatio = if (height == 0) 0f else width.toFloat() / height.toFloat()
            updateSurfaceSize()
        }

        // 媒体播放完毕时调用
        override fun onCompletion(mp: MediaPlayer?) {
            mPlayPauseButton.setImageResource(R.drawable.ic_play_white)
        }

        // 播放进度改变时调用
        override fun onProgress(position: Int, duration: Int) {
            if (mSliderTrackingTouch) return
            mSlider.valueTo = duration.toFloat().coerceAtLeast(1f)
            mSlider.value = position.toFloat().coerceAtLeast(mSlider.valueFrom).coerceAtMost(mSlider.valueTo)
        }
    }

    // 连接MediaService的对象
    val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mMediaBinder = service as MediaService.MediaBinder
            mMediaBinder.setMediaListener(mMediaListener)
            mSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    mMediaBinder.setDisplay(holder)
                }
                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                    Log.d(TAG, "Surface changed! width=$width, height=$height")
                }
                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    mMediaBinder.setDisplay(null)
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) { }
    }

    private var mMediaControlViewVisibilityChangeTime = System.currentTimeMillis()
    private val mHideMediaControlRunnable = Runnable { mMediaControlView.visibility = View.INVISIBLE }

    // 媒体播放工具栏显示与自动隐藏
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val currentTime = System.currentTimeMillis()
        if ((currentTime - mMediaControlViewVisibilityChangeTime > 100)) {
            mMediaControlView.handler.removeCallbacks(mHideMediaControlRunnable)
            val xy = IntArray(2)
            mMediaControlView.getLocationInWindow(xy)
            val rect = Rect(xy[0], xy[1], xy[0] + mMediaControlView.width, xy[1] + mMediaControlView.height)
            val touchInMediaControlView = rect.contains(ev.x.toInt(), ev.y.toInt())
            if (mMediaControlView.visibility == View.VISIBLE && !touchInMediaControlView) {
                mMediaControlView.visibility = View.INVISIBLE
            } else {
                mMediaControlView.visibility = View.VISIBLE
                mMediaControlView.bringToFront()
                mMediaControlView.handler.postDelayed(mHideMediaControlRunnable, 2000)
            }
            mMediaControlViewVisibilityChangeTime = currentTime
        }
        return super.dispatchTouchEvent(ev)
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
        mMediaControlView = findViewById(R.id.media_control)
        mPlayPauseButton = findViewById(R.id.play_pause_media_button)
        mSlider = findViewById(R.id.slider)

        // 保证转屏时可以正确更新SurfaceView的尺寸
        mSurfaceView.viewTreeObserver.addOnGlobalLayoutListener { updateSurfaceSize() }

        // 连接绑定MediaService
        val bindIntent = Intent(this, MediaService::class.java)
        bindService(bindIntent, mServiceConnection, BIND_AUTO_CREATE)

        // 打开媒体文件按钮
        findViewById<ImageButton>(R.id.open_media_file_button).setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(this, 0)
            }
        }

        // 播放/暂停按钮
        mPlayPauseButton.setOnClickListener {
            if (mMediaBinder.playOrPause()) mPlayPauseButton.setImageResource(R.drawable.ic_pause_white)
            else mPlayPauseButton.setImageResource(R.drawable.ic_play_white)
        }

        // 停止按钮
        findViewById<ImageButton>(R.id.stop_media_button).setOnClickListener {
            mMediaBinder.stop()
            mPlayPauseButton.setImageResource(R.drawable.ic_play_white)
        }

        // 按住进度条时，让进度条显示格式为（分:秒.毫秒）的时间
        mSlider.setLabelFormatter { value: Float ->
            val millis = value.toInt()
            val seconds = millis / 1000
            val minutes = seconds / 60
            "$minutes:${seconds % 60}.${millis % 1000}"
        }

        // 更新mSliderTrackingTouch，使得我们通过这个变量可以得知当前进度条是否正在被拖
        mSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) { mSliderTrackingTouch = true }
            override fun onStopTrackingTouch(slider: Slider) { mSliderTrackingTouch = false }
        })

        // 通过进度条调整播放进度
        mSlider.addOnChangeListener { slider, value, fromUser ->
            if (mSliderTrackingTouch and fromUser) {
                mMediaBinder.seekTo(value.toInt())
            }
        }
    }

    // 得到打开媒体文件结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri: Uri = data?.data ?: return
        mMediaBinder.open(uri)  // 要MediaService开始播放媒体文件
        mPlayPauseButton.setImageResource(R.drawable.ic_pause_white)
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
        mMediaBinder.setMediaListener(null)
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