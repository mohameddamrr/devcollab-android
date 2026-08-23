package com.mohamedamr.devcollab.feature.discover

import com.mohamedamr.devcollab.domain.model.DeveloperSummary

data class SearchUiState(
    val query: String = "",
    val result: SearchResultUiState = SearchResultUiState.Initial,
)

sealed interface SearchResultUiState {
    data object Initial : SearchResultUiState

    data object Loading : SearchResultUiState

    data class Success(
        val developers: List<DeveloperSummary>,
        val totalCount: Int,
    ) : SearchResultUiState

    data object Empty : SearchResultUiState

    data class Error(val reason: SearchErrorReason) : SearchResultUiState
}

sealed interface SearchErrorReason {
    data object EmptyQuery : SearchErrorReason
    data object NetworkUnavailable : SearchErrorReason
    data class RateLimited(val resetAtEpochSeconds: Long?) : SearchErrorReason
    data class Server(val statusCode: Int) : SearchErrorReason
    data object InvalidData : SearchErrorReason
    data object Unexpected : SearchErrorReason
}
