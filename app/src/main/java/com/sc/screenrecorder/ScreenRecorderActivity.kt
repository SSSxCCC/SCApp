package com.sc.screenrecorder

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sc.scapp.R

class ScreenRecorderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_recorder)
    }

    companion object {
        fun actionStart(context: Context) {
            val intent = Intent(context, ScreenRecorderActivity::class.java)
            context.startActivity(intent)
        }
    }
}