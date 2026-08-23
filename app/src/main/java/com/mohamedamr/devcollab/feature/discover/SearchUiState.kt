package com.mohamedamr.devcollab.feature.discover

data class SearchUiState(
    val query: String = "",
    val hasSubmittedSearch: Boolean = false,
    val validationError: SearchValidationError? = null,
)

sealed interface SearchValidationError {
    data object EmptyQuery : SearchValidationError
}

sealed interface SearchErrorReason {
    data object EmptyQuery : SearchErrorReason
    data object NetworkUnavailable : SearchErrorReason
    data class RateLimited(val resetAtEpochSeconds: Long?) : SearchErrorReason
    data class Server(val statusCode: Int) : SearchErrorReason
    data object InvalidData : SearchErrorReason
    data object Unexpected : SearchErrorReason
}
