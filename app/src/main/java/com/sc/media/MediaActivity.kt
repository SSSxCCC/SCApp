package com.sc.media

import android.app.Activity
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
import com.sc.scapp.R

class MediaActivity : Activity(), MediaPlayer.OnVideoSizeChangedListener {
    lateinit var mSurfaceContainer: FrameLayout
    lateinit var mSurfaceView: SurfaceView
    lateinit var mMediaBinder: MediaService.MediaBinder
    var mVideoRatio = 0f

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

        mSurfaceContainer = findViewById(R.id.surface_container)
        mSurfaceView = findViewById(R.id.surface_view)
        mSurfaceView.viewTreeObserver.addOnGlobalLayoutListener { updateSurfaceSize(mVideoRatio) }

        val bindIntent = Intent(this, MediaService::class.java)
        bindService(bindIntent, mServiceConnection, BIND_AUTO_CREATE)

        val openMediaFileButton = findViewById<Button>(R.id.open_media_file_button)
        openMediaFileButton.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                startActivityForResult(this, 0)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri: Uri = data?.data ?: return
        mMediaBinder.open(uri)
    }

    override fun onVideoSizeChanged(mp: MediaPlayer?, width: Int, height: Int) {
        mVideoRatio = if (height == 0) 0f else width.toFloat() / height.toFloat()
        updateSurfaceSize(mVideoRatio)
    }

    private fun updateSurfaceSize(videoRatio: Float) {
        if (videoRatio <= 0) return
        val playerRatio = mSurfaceContainer.width.toFloat() / mSurfaceContainer.height.toFloat()
        val params = mSurfaceView.layoutParams
        if (playerRatio > videoRatio) {
            params.width = (mSurfaceContainer.height.toFloat() * videoRatio).toInt()
            params.height = mSurfaceContainer.height
        } else if (playerRatio < videoRatio) {
            params.width = mSurfaceContainer.width
            params.height = (mSurfaceContainer.width.toFloat() / videoRatio).toInt()
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