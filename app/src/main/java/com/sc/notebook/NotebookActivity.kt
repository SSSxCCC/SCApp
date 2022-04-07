package com.sc.notebook

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class NotebookActivity : AppCompatActivity() {
    private val noteViewModel: NoteViewModel by viewModels { NoteViewModelFactory((application as NotebookApplication).noteRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainContent(noteViewModel)
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
private fun PreviewContent() {
    RootContent(notes = listOf(Note(1, "11", "111"),
        Note(2, "22", "222"),
        Note(3, "33", "333"),), {Note(0, "", "")})
}

@Composable
private fun MainContent(noteViewModel: NoteViewModel) {
    val notes by noteViewModel.allNotes.observeAsState(listOf())
    RootContent(notes, { noteViewModel.insertBlock(Note(0, "Title", "Content")) })
}

@Composable
private fun RootContent(notes: List<Note>, onAddNote: () -> Note) {
    val context = LocalContext.current
    Scaffold(floatingActionButton = {
        Icon(Icons.Rounded.Add, "New note", Modifier.size(100.dp).clickable {
            val note = onAddNote()
            Log.e("AAABBBCCC", "id=" + note.id)
            NoteActivity.actionStart(context, note)
        })
    }, content = {
        NoteList(notes = notes)
    })
}

@Composable
private fun NoteList(notes: List<Note>) {
    val context = LocalContext.current
    LazyColumn {
        items(notes) { note ->
            Text(text = note.title,
                Modifier.fillMaxWidth()
                    .padding(all = 30.dp)
                    .clickable { NoteActivity.actionStart(context, note) })
        }
    }
}