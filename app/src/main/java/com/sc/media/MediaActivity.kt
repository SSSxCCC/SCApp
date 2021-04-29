package com.sc.media

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import com.sc.scapp.R

class MediaActivity : AppCompatActivity() {
    var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)

        val openMediaFileButton = findViewById<Button>(R.id.open_media_file_button)
        openMediaFileButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri: Uri = data?.data ?: return
        val surfaceView = findViewById<SurfaceView>(R.id.surface_view)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mediaPlayer?.apply {
                    stop()
                    release()
                }
                mediaPlayer = MediaPlayer.create(this@MediaActivity, uri, holder)
                mediaPlayer?.start()
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { }
            override fun surfaceDestroyed(holder: SurfaceHolder) { }
        })
    }

    companion object {
        private val TAG = MediaActivity::class.simpleName!!

        fun actionStart(context: Context) {
            val intent = Intent(context, MediaActivity::class.java)
            context.startActivity(intent)
        }
    }
}