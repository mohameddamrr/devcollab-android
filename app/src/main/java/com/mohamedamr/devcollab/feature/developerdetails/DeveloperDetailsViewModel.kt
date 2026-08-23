package com.mohamedamr.devcollab.feature.developerdetails

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

class DeveloperDetailsViewModel(
    username: String,
    private val developerRepository: DeveloperRepository,
) : ViewModel() {
    private val normalizedUsername = username.trim()

    private val _uiState = MutableStateFlow(
        DeveloperDetailsUiState(username = normalizedUsername),
    )
    val uiState: StateFlow<DeveloperDetailsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadProfile()
    }

    fun retry() {
        loadProfile()
    }

    private fun loadProfile() {
        if (normalizedUsername.isEmpty()) {
            _uiState.update {
                it.copy(
                    result = DeveloperDetailsResultUiState.Error(
                        DeveloperDetailsErrorReason.InvalidUsername,
                    ),
                )
            }
            return
        }
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(result = DeveloperDetailsResultUiState.Loading) }

            val repositoryResult = try {
                developerRepository.getDeveloperProfile(normalizedUsername)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                DeveloperRepositoryResult.Failure(DeveloperRepositoryError.Unexpected)
            }

            _uiState.update { currentState ->
                currentState.copy(
                    result = when (repositoryResult) {
                        is DeveloperRepositoryResult.Success -> {
                            DeveloperDetailsResultUiState.Success(repositoryResult.data)
                        }
                        is DeveloperRepositoryResult.Failure -> {
                            DeveloperDetailsResultUiState.Error(
                                repositoryResult.error.toDetailsReason(),
                            )
                        }
                    },
                )
            }
        }
    }
}

private fun DeveloperRepositoryError.toDetailsReason(): DeveloperDetailsErrorReason = when (this) {
    DeveloperRepositoryError.NetworkUnavailable -> {
        DeveloperDetailsErrorReason.NetworkUnavailable
    }
    is DeveloperRepositoryError.RateLimited -> {
        DeveloperDetailsErrorReason.RateLimited(resetAtEpochSeconds)
    }
    is DeveloperRepositoryError.Server -> {
        if (statusCode == HTTP_NOT_FOUND) {
            DeveloperDetailsErrorReason.NotFound
        } else {
            DeveloperDetailsErrorReason.Server(statusCode)
        }
    }
    DeveloperRepositoryError.InvalidData -> DeveloperDetailsErrorReason.InvalidData
    DeveloperRepositoryError.Unexpected -> DeveloperDetailsErrorReason.Unexpected
}

private const val HTTP_NOT_FOUND = 404
