package com.mohamedamr.devcollab.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val developerRepository: DeveloperRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        if (query == _uiState.value.query) return

        searchJob?.cancel()
        searchJob = null
        _uiState.update { currentState ->
            currentState.copy(query = query, result = SearchResultUiState.Initial)
        }
    }

    fun search() {
        val normalizedQuery = _uiState.value.query.trim()
        if (normalizedQuery.isEmpty()) {
            _uiState.update {
                it.copy(result = SearchResultUiState.Error(SearchErrorReason.EmptyQuery))
            }
            return
        }
        if (searchJob?.isActive == true && _uiState.value.result == SearchResultUiState.Loading) {
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    query = normalizedQuery,
                    result = SearchResultUiState.Loading,
                )
            }

            val repositoryResult = try {
                developerRepository.searchDevelopers(normalizedQuery)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                DeveloperRepositoryResult.Failure(DeveloperRepositoryError.Unexpected)
            }

            when (val result = repositoryResult) {
                is DeveloperRepositoryResult.Success -> {
                    val page = result.data
                    _uiState.update {
                        it.copy(
                            result = if (page.developers.isEmpty()) {
                                SearchResultUiState.Empty
                            } else {
                                SearchResultUiState.Success(
                                    developers = page.developers,
                                    totalCount = page.totalCount,
                                )
                            },
                        )
                    }
                }

                is DeveloperRepositoryResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            result = SearchResultUiState.Error(result.error.toUiReason()),
                        )
                    }
                }
            }
        }
    }
}

private fun DeveloperRepositoryError.toUiReason(): SearchErrorReason = when (this) {
    DeveloperRepositoryError.NetworkUnavailable -> SearchErrorReason.NetworkUnavailable
    is DeveloperRepositoryError.RateLimited -> SearchErrorReason.RateLimited(resetAtEpochSeconds)
    is DeveloperRepositoryError.Server -> SearchErrorReason.Server(statusCode)
    DeveloperRepositoryError.InvalidData -> SearchErrorReason.InvalidData
    DeveloperRepositoryError.Unexpected -> SearchErrorReason.Unexpected
}
