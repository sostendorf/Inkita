package net.dom53.inkita.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.dom53.inkita.domain.model.SearchHit
import net.dom53.inkita.domain.repository.SeriesRepository

data class SearchUiState(
    val query: String = "",
    val results: List<SearchHit> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null,
)

/** Debounce before querying, so typing does not fire a request per keystroke. */
private const val SEARCH_DEBOUNCE_MS = 300L

/** Below this, matches are too broad to be worth a round trip. */
private const val MIN_QUERY_LENGTH = 2

class SearchViewModel(
    private val seriesRepository: SeriesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, error = null) }
        // Each keystroke replaces the pending search, so only the last one runs.
        searchJob?.cancel()
        if (query.trim().length < MIN_QUERY_LENGTH) {
            _state.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        searchJob =
            viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                _state.update { it.copy(isSearching = true) }
                runCatching { seriesRepository.searchSeries(query) }
                    .onSuccess { hits ->
                        _state.update { it.copy(results = hits, isSearching = false, error = null) }
                    }.onFailure { e ->
                        _state.update {
                            it.copy(
                                results = emptyList(),
                                isSearching = false,
                                error = e.message ?: "Search failed",
                            )
                        }
                    }
            }
    }

    companion object {
        fun provideFactory(seriesRepository: SeriesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return SearchViewModel(seriesRepository) as T
                }
            }
    }
}
