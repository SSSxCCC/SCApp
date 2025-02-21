package com.sc.opengl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sc.scapp.R

class OpenGLActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opengl)
    }

    companion object {
        private val TAG = OpenGLActivity::class.simpleName!!

        fun actionStart(context: Context) {
            val intent = Intent(context, OpenGLActivity::class.java)
            context.startActivity(intent)
        }
    }
}