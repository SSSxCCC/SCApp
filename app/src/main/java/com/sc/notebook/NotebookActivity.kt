package com.sc.notebook

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class NotebookActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainContent()
        }
    }

    companion object {
        fun actionStart(context: Context) {
            val intent = Intent(context, NotebookActivity::class.java)
            context.startActivity(intent)
        }
    }
}

@Preview
@Composable
fun MainContent() {
    Text(text = "Notebook!")
}