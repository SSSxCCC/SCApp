package com.sc.notebook

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class NotebookActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PreviewContent()
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
    val context = LocalContext.current
    Scaffold(floatingActionButton = {
        Icon(Icons.Rounded.Add, "New note", Modifier.clickable {
            NoteActivity.actionStart(context, Note(100, "title", "content"))
        })
    }, content = {
        NoteList(notes = listOf(Note(1, "11", "111"),
            Note(2, "22", "222"),
            Note(3, "33", "333"),))
    })
}

@Composable
fun NoteList(notes: List<Note>) {
    val context = LocalContext.current
    LazyColumn {
        items(notes) { note ->
            Text(text = note.title,
                Modifier
                    .padding(all = 30.dp)
                    .clickable { NoteActivity.actionStart(context, note) })
        }
    }
}