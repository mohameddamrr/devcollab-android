package com.mohamedamr.devcollab.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.domain.model.SearchDataStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val developerRepository: DeveloperRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val searchRequest = MutableStateFlow<SearchRequest?>(null)
    val pagingData: Flow<PagingData<DeveloperSummary>> = searchRequest
        .flatMapLatest { request ->
            if (request == null) {
                flowOf(PagingData.empty())
            } else {
                developerRepository.getPagedDevelopers(request.query)
            }
        }
        .cachedIn(viewModelScope)

    private var debounceJob: Job? = null
    private var requestId = 0L

    init {
        viewModelScope.launch {
            developerRepository.searchDataStatus.collect { status ->
                _uiState.update { state ->
                    state.copy(
                        cachedAtEpochMillis = (status as? SearchDataStatus.Cached)
                            ?.cachedAtEpochMillis,
                    )
                }
            }
        }
        viewModelScope.launch {
            developerRepository.getLastSearch()?.let { lastSearch ->
                _uiState.value = SearchUiState(query = lastSearch.query)
                submitSearch()
            }
        }
    }

    fun onQueryChanged(query: String) {
        if (query == _uiState.value.query) return

        debounceJob?.cancel()
        debounceJob = null
        searchRequest.value = null
        _uiState.value = SearchUiState(query = query)

        if (query.trim().length >= MINIMUM_AUTO_SEARCH_LENGTH) {
            debounceJob = viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MILLIS)
                submitSearch()
            }
        }
    }

    fun search() {
        debounceJob?.cancel()
        debounceJob = null
        submitSearch()
    }

    private fun submitSearch() {
        val normalizedQuery = _uiState.value.query.trim()
        if (normalizedQuery.isEmpty()) {
            searchRequest.value = null
            _uiState.update { it.copy(validationError = SearchValidationError.EmptyQuery) }
            return
        }

        val newRequestId = ++requestId
        _uiState.update {
            it.copy(
                query = normalizedQuery,
                hasSubmittedSearch = true,
                validationError = null,
            )
        }
        searchRequest.value = SearchRequest(id = newRequestId, query = normalizedQuery)
    }
}

private data class SearchRequest(
    val id: Long,
    val query: String,
)

internal const val SEARCH_DEBOUNCE_MILLIS = 500L
internal const val MINIMUM_AUTO_SEARCH_LENGTH = 3
