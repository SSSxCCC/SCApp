package com.sc.notebook

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    RootContent(note = Note(1, "title", "content"), {}, {})
}

@Composable
private fun MainContent(note: Note, noteViewModel: NoteViewModel) {
    RootContent(note, { note -> noteViewModel.upate(note) }, { note -> noteViewModel.delete(note) })
}

@Composable
private fun RootContent(note: Note, onNoteChange: (note: Note) -> Unit, deleteNote: (note: Note) -> Unit) {
    val activity = LocalContext.current as Activity
    var openConfirmDeleteDialog by remember { mutableStateOf(false) }
    Column {
        var title by remember { mutableStateOf(note.title) }
        var content by remember { mutableStateOf(note.content) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(value = title, onValueChange = { title = it; onNoteChange(Note(note.id, title, content)) }, Modifier.weight(7f))
            Icon(Icons.Filled.Delete, "Delete note",
                Modifier.weight(1f).clickable { openConfirmDeleteDialog = true })
        }
        TextField(value = content, onValueChange = { content = it; onNoteChange(Note(note.id, title, content)) }, Modifier.fillMaxSize())
    }
    if (openConfirmDeleteDialog) {
        AlertDialog(onDismissRequest = { openConfirmDeleteDialog = false },
            title = { Text("Confirm delete note") },
            confirmButton = { Button(onClick = { openConfirmDeleteDialog = false; deleteNote(note); activity.finish() }) { Text("Delete") } },
            dismissButton = { Button(onClick = { openConfirmDeleteDialog = false }) { Text("Dismiss") } })
    }
}