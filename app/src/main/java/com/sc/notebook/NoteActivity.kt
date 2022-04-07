package com.sc.notebook

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

class NoteActivity : AppCompatActivity() {
    private val noteViewModel: NoteViewModel by viewModels { NoteViewModelFactory((application as NotebookApplication).noteRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val note = intent.extras!!["note"] as Note
        setContent {
            MainContent(note, noteViewModel)
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
    RootContent(note = Note(1, "title", "content"), {})
}

@Composable
private fun MainContent(note: Note, noteViewModel: NoteViewModel) {
    RootContent(note, { note -> noteViewModel.upate(note) })
}

@Composable
private fun RootContent(note: Note, onNoteChange: (note: Note) -> Unit) {
    Column {
        var title by remember { mutableStateOf(note.title) }
        TextField(value = title, onValueChange = { title = it; onNoteChange(Note(note.id, title, note.content)) }, modifier = Modifier.fillMaxWidth())
        var content by remember { mutableStateOf(note.content) }
        TextField(value = content, onValueChange = { content = it; onNoteChange(Note(note.id, note.title, content)) }, modifier = Modifier.fillMaxSize())
    }
}