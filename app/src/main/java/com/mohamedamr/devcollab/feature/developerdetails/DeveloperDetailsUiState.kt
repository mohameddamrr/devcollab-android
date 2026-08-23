package com.mohamedamr.devcollab.feature.developerdetails

import com.mohamedamr.devcollab.domain.model.DeveloperProfile

data class DeveloperDetailsUiState(
    val username: String,
    val result: DeveloperDetailsResultUiState = DeveloperDetailsResultUiState.Loading,
)

sealed interface DeveloperDetailsResultUiState {
    data object Loading : DeveloperDetailsResultUiState

    data class Success(
        val profile: DeveloperProfile,
    ) : DeveloperDetailsResultUiState

    data class Error(
        val reason: DeveloperDetailsErrorReason,
    ) : DeveloperDetailsResultUiState
}

sealed interface DeveloperDetailsErrorReason {
    data object InvalidUsername : DeveloperDetailsErrorReason
    data object NotFound : DeveloperDetailsErrorReason
    data object NetworkUnavailable : DeveloperDetailsErrorReason
    data class RateLimited(val resetAtEpochSeconds: Long?) : DeveloperDetailsErrorReason
    data class Server(val statusCode: Int) : DeveloperDetailsErrorReason
    data object InvalidData : DeveloperDetailsErrorReason
    data object Unexpected : DeveloperDetailsErrorReason
}
