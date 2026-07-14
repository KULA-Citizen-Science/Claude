package com.kula.stylusnotes.ui.notelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kula.stylusnotes.data.repository.NoteRepository
import com.kula.stylusnotes.data.repository.NoteSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteListViewModel(private val repository: NoteRepository) : ViewModel() {

    val notes: StateFlow<List<NoteSummary>> = repository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createNote(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val id = repository.createNote()
            onCreated(id)
        }
    }

    fun renameNote(noteId: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.renameNote(noteId, trimmed) }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch { repository.deleteNote(noteId) }
    }
}
