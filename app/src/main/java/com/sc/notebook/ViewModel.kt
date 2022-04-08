package com.sc.notebook

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NoteViewModel(private val noteRepository: NoteRepository) : ViewModel() {
    val allNotes: LiveData<List<Note>> = noteRepository.allNotes.asLiveData()

    fun insertBlock(note: Note): Note = runBlocking {
        val id = noteRepository.insert(note)
        return@runBlocking Note(id, note.title, note.content)
    }

    fun insert(note: Note) = viewModelScope.launch {
        noteRepository.insert(note)
    }

    fun upate(note: Note) = viewModelScope.launch {
        noteRepository.update(note)
    }

    fun delete(note: Note) = viewModelScope.launch {
        noteRepository.delete(note)
    }
}

class NoteViewModelFactory(private val noteRepository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            return NoteViewModel(noteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
