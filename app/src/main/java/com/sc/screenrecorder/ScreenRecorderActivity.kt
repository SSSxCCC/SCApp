package com.sc.screenrecorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.sc.scapp.R

class ScreenRecorderActivity : AppCompatActivity() {
    private lateinit var mMediaProjectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_recorder)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 0)
        }

        mMediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        findViewById<Button>(R.id.start_record_screen_button).setOnClickListener {
            val captureIntent = mMediaProjectionManager.createScreenCaptureIntent()
            startActivityForResult(captureIntent, 0)
        }

        findViewById<Button>(R.id.stop_record_screen_button).setOnClickListener {
            stopService(Intent(this, ScreenRecorderService::class.java))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (gr in grantResults) {
            if (gr != PackageManager.PERMISSION_GRANTED) {
                finish()
                break
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        ScreenRecorderService.actionStart(this, resultCode, data)
    }

    companion object {

        private val TAG = ScreenRecorderActivity::class.simpleName!!

        fun actionStart(context: Context) {
            val intent = Intent(context, ScreenRecorderActivity::class.java)
            context.startActivity(intent)
        }
    }
}