package com.sc.notebook

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class NotebookApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())
    val noteDatabase by lazy { NoteDatabase.getInstance(this, applicationScope) }
    val noteRepository by lazy { NoteRepository(noteDatabase.noteDao()) }
}