package com.sc.notebook

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class NoteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val note = intent.extras!!["note"] as Note
        setContent {
            NoteContent(note)
        }
    }

    companion object {
        fun actionStart(context: Context, note: Note) {
            val intent = Intent(context, NoteActivity::class.java)
            intent.putExtra("note", note)
            context.startActivity(intent)
        }
    }
}

@Preview
@Composable
private fun PreviewContent() {
    NoteContent(note = Note(1, "title", "content"))
}

@Composable
private fun NoteContent(note: Note) {
    Column {
        Text(note.title)
        Text(note.content)
    }
}