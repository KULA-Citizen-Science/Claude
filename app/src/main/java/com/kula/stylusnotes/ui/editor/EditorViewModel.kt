package com.kula.stylusnotes.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kula.stylusnotes.core.model.CanvasBackground
import com.kula.stylusnotes.core.model.Stroke
import com.kula.stylusnotes.data.repository.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditorUiState(
    val noteId: String? = null,
    val title: String = "",
    val background: CanvasBackground = CanvasBackground.WHITE,
    val strokes: List<Stroke> = emptyList(),
    val isLoaded: Boolean = false
)

class EditorViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var autosaveJob: Job? = null

    fun load(noteId: String) {
        if (_uiState.value.noteId == noteId) return
        viewModelScope.launch {
            val note = repository.loadNote(noteId) ?: return@launch
            _uiState.value = EditorUiState(
                noteId = note.id,
                title = note.title,
                background = note.background,
                strokes = note.strokes,
                isLoaded = true
            )
        }
    }

    fun onBackgroundToggled(background: CanvasBackground) {
        _uiState.value = _uiState.value.copy(background = background)
        persist(debounce = false)
    }

    fun onStrokesChanged(strokes: List<Stroke>) {
        _uiState.value = _uiState.value.copy(strokes = strokes)
        persist(debounce = true)
    }

    /** Cancels any pending debounced save and persists immediately; call from onPause/onDispose. */
    fun saveNow() {
        autosaveJob?.cancel()
        val state = _uiState.value
        val noteId = state.noteId ?: return
        viewModelScope.launch { repository.saveStrokes(noteId, state.background, state.strokes) }
    }

    private fun persist(debounce: Boolean) {
        val state = _uiState.value
        val noteId = state.noteId ?: return
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            if (debounce) delay(AUTOSAVE_DEBOUNCE_MS)
            repository.saveStrokes(noteId, state.background, state.strokes)
        }
    }

    companion object {
        private const val AUTOSAVE_DEBOUNCE_MS = 600L
    }
}
