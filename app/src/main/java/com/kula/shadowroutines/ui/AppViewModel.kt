package com.kula.shadowroutines.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.kula.shadowroutines.ShadowRoutinesApp
import com.kula.shadowroutines.core.routine.RoutineTemplate
import com.kula.shadowroutines.data.db.RoutineWithItems
import com.kula.shadowroutines.data.repository.QuoteRepository
import com.kula.shadowroutines.data.repository.RewardQuote
import com.kula.shadowroutines.data.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Aggregate counters for the gentle gamification row. */
data class Stats(val completedRoutines: Int = 0, val uniqueQuotesSeen: Int = 0)

/** State backing the full-screen reward card. [quote] is null when the corpus is empty. */
data class RewardUiState(val taskTitle: String, val quote: RewardQuote?)

/**
 * The single ViewModel for v0. Owns routine/quote operations and the reward state. Screens are
 * stateless and receive state + callbacks from here.
 */
class AppViewModel(
    private val routineRepo: RoutineRepository,
    private val quoteRepo: QuoteRepository,
) : ViewModel() {

    val templates: List<RoutineTemplate> get() = routineRepo.templates

    val activeRoutines: StateFlow<List<RoutineWithItems>> =
        routineRepo.activeRoutines()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<Stats> =
        combine(routineRepo.completedCount(), quoteRepo.uniqueSeenCount()) { completed, unique ->
            Stats(completed, unique)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats())

    private val _reward = MutableStateFlow<RewardUiState?>(null)
    val reward: StateFlow<RewardUiState?> = _reward.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    fun routine(id: Long): Flow<RoutineWithItems?> = routineRepo.observeRoutine(id)

    fun startRoutine(templateId: String, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            val title = routineRepo.template(templateId)?.name ?: "Task"
            onStarted(routineRepo.start(templateId, title, description = null))
        }
    }

    fun setItemChecked(itemId: Long, checked: Boolean) {
        viewModelScope.launch { routineRepo.setItemChecked(itemId, checked) }
    }

    fun completeRoutine(routineId: Long) {
        viewModelScope.launch {
            val completed = routineRepo.complete(routineId) ?: return@launch
            val quote = quoteRepo.selectReward(completed.query)
            _reward.value = RewardUiState(completed.title, quote)
        }
    }

    fun dismissReward() {
        _reward.value = null
    }

    fun toggleFavorite() {
        val current = _reward.value ?: return
        val quote = current.quote ?: return
        viewModelScope.launch {
            val newValue = !quote.isFavorite
            quoteRepo.setFavorite(quote.id, newValue)
            _reward.value = current.copy(quote = quote.copy(isFavorite = newValue))
        }
    }

    fun rescanCorpus() {
        viewModelScope.launch {
            val count = quoteRepo.rescanFromAssets()
            _messages.emit("Rescanned bundled corpus — $count quotes")
        }
    }

    fun importCorpus(treeUri: Uri) {
        viewModelScope.launch {
            val count = quoteRepo.importFromDirectory(treeUri)
            _messages.emit(
                if (count > 0) "Imported $count quotes" else "No .md quotes found in that folder",
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ShadowRoutinesApp
                AppViewModel(app.container.routineRepository, app.container.quoteRepository)
            }
        }
    }
}
