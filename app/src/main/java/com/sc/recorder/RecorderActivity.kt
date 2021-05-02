package com.sc.recorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.sc.scapp.R

class RecorderActivity : AppCompatActivity() {
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private lateinit var spinner: Spinner

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
        spinner = findViewById(R.id.spinner)

        findViewById<Button>(R.id.start_record_button).setOnClickListener {
            when ((spinner.selectedView as TextView).text) {
                "Screen (mp4)" -> {
                    val captureIntent = mMediaProjectionManager.createScreenCaptureIntent()
                    startActivityForResult(captureIntent, 0)
                }
                "Mic (3gp)" -> {
                    RecorderService.actionStartMic(this)
                }
            }
        }

        findViewById<Button>(R.id.stop_record_button).setOnClickListener {
            stopService(Intent(this, RecorderService::class.java))
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
        RecorderService.actionStartScreen(this, resultCode, data)
    }

    companion object {
        private val TAG = RecorderActivity::class.simpleName!!

        fun actionStart(context: Context) {
            val intent = Intent(context, RecorderActivity::class.java)
            context.startActivity(intent)
        }
    }
}