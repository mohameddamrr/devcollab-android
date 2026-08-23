package com.mohamedamr.devcollab.feature.developerdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
    private var repositoryLoadJob: Job? = null
    private var activityLoadJob: Job? = null
    private var nextRepositoryPage = FIRST_REPOSITORY_PAGE

    init {
        loadProfile()
        loadRepositories()
        loadActivity()
    }

    fun retry() {
        loadProfile()
    }

    fun selectTab(tab: DeveloperProfileTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun retryRepositories() {
        val currentRepositories = _uiState.value.repositories
        if (currentRepositories is DeveloperRepositoriesUiState.Success &&
            currentRepositories.appendError != null
        ) {
            loadMoreRepositories()
        } else {
            loadRepositories(reset = true)
        }
    }

    fun retryActivity() {
        loadActivity()
    }

    fun loadMoreRepositories() {
        val currentRepositories = _uiState.value.repositories
            as? DeveloperRepositoriesUiState.Success ?: return
        if (!currentRepositories.canLoadMore || currentRepositories.isLoadingMore) return
        loadRepositories(reset = false)
    }

    fun onRepositoryQueryChanged(query: String) {
        _uiState.update { it.copy(repositoryQuery = query) }
    }

    fun onRepositorySortChanged(sort: RepositorySort) {
        _uiState.update { it.copy(repositorySort = sort) }
    }

    fun refresh() {
        if (
            normalizedUsername.isEmpty() ||
            loadJob?.isActive == true ||
            repositoryLoadJob?.isActive == true ||
            activityLoadJob?.isActive == true ||
            _uiState.value.isRefreshing
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, refreshError = null) }

            val profileDeferred = async { fetchProfile() }
            val repositoriesDeferred = async {
                fetchRepositories(FIRST_REPOSITORY_PAGE)
            }
            val activityDeferred = async { fetchActivity() }
            val profileResult = profileDeferred.await()
            val repositoriesResult = repositoriesDeferred.await()
            val activityResult = activityDeferred.await()

            if (repositoriesResult is DeveloperRepositoryResult.Success) {
                nextRepositoryPage = FIRST_REPOSITORY_PAGE + 1
            }

            _uiState.update { currentState ->
                val refreshError = listOfNotNull(
                    (profileResult as? DeveloperRepositoryResult.Failure)?.error,
                    (repositoriesResult as? DeveloperRepositoryResult.Failure)?.error,
                    (activityResult as? DeveloperRepositoryResult.Failure)?.error,
                ).firstOrNull()?.toDetailsReason()

                currentState.copy(
                    result = when (profileResult) {
                        is DeveloperRepositoryResult.Success -> {
                            DeveloperDetailsResultUiState.Success(profileResult.data)
                        }
                        is DeveloperRepositoryResult.Failure -> currentState.result
                    },
                    repositories = when (repositoriesResult) {
                        is DeveloperRepositoryResult.Success -> {
                            DeveloperRepositoriesUiState.Success(
                                repositories = repositoriesResult.data,
                                canLoadMore = repositoriesResult.data.size == REPOSITORY_PAGE_SIZE,
                            )
                        }
                        is DeveloperRepositoryResult.Failure -> currentState.repositories
                    },
                    activity = when (activityResult) {
                        is DeveloperRepositoryResult.Success -> {
                            DeveloperActivityUiState.Success(activityResult.data)
                        }
                        is DeveloperRepositoryResult.Failure -> currentState.activity
                    },
                    isRefreshing = false,
                    refreshError = refreshError,
                )
            }
        }
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

            val repositoryResult = fetchProfile()

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

    private fun loadRepositories(reset: Boolean = true) {
        if (normalizedUsername.isEmpty() || repositoryLoadJob?.isActive == true) return

        val currentRepositories = _uiState.value.repositories
        val pageToLoad = if (reset) FIRST_REPOSITORY_PAGE else nextRepositoryPage

        repositoryLoadJob = viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    repositories = if (reset) {
                        DeveloperRepositoriesUiState.Loading
                    } else {
                        (currentRepositories as DeveloperRepositoriesUiState.Success).copy(
                            isLoadingMore = true,
                            appendError = null,
                        )
                    },
                )
            }

            val repositoryResult = fetchRepositories(pageToLoad)

            _uiState.update { currentState ->
                currentState.copy(
                    repositories = when (repositoryResult) {
                        is DeveloperRepositoryResult.Success -> {
                            val previous = if (reset) {
                                emptyList()
                            } else {
                                (currentRepositories as DeveloperRepositoriesUiState.Success)
                                    .repositories
                            }
                            val combined = (previous + repositoryResult.data)
                                .distinctBy { it.githubId }
                            nextRepositoryPage = pageToLoad + 1
                            DeveloperRepositoriesUiState.Success(
                                repositories = combined,
                                canLoadMore = repositoryResult.data.size == REPOSITORY_PAGE_SIZE,
                            )
                        }
                        is DeveloperRepositoryResult.Failure -> {
                            val reason = repositoryResult.error.toDetailsReason()
                            if (reset) {
                                DeveloperRepositoriesUiState.Error(reason)
                            } else {
                                (currentRepositories as DeveloperRepositoriesUiState.Success).copy(
                                    isLoadingMore = false,
                                    appendError = reason,
                                )
                            }
                        }
                    },
                )
            }
        }
    }

    private fun loadActivity() {
        if (normalizedUsername.isEmpty() || activityLoadJob?.isActive == true) return

        activityLoadJob = viewModelScope.launch {
            _uiState.update { it.copy(activity = DeveloperActivityUiState.Loading) }

            val activityResult = fetchActivity()
            _uiState.update { currentState ->
                currentState.copy(
                    activity = when (activityResult) {
                        is DeveloperRepositoryResult.Success -> {
                            DeveloperActivityUiState.Success(activityResult.data)
                        }
                        is DeveloperRepositoryResult.Failure -> {
                            DeveloperActivityUiState.Error(
                                activityResult.error.toDetailsReason(),
                            )
                        }
                    },
                )
            }
        }
    }

    private suspend fun fetchProfile() = try {
        developerRepository.getDeveloperProfile(normalizedUsername)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        DeveloperRepositoryResult.Failure(DeveloperRepositoryError.Unexpected)
    }

    private suspend fun fetchRepositories(page: Int) = try {
        developerRepository.getDeveloperRepositories(
            username = normalizedUsername,
            page = page,
            pageSize = REPOSITORY_PAGE_SIZE,
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        DeveloperRepositoryResult.Failure(DeveloperRepositoryError.Unexpected)
    }

    private suspend fun fetchActivity() = try {
        developerRepository.getDeveloperRecentActivity(
            username = normalizedUsername,
            pageSize = ACTIVITY_PAGE_SIZE,
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        DeveloperRepositoryResult.Failure(DeveloperRepositoryError.Unexpected)
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
private const val FIRST_REPOSITORY_PAGE = 1
private const val REPOSITORY_PAGE_SIZE = 30
private const val ACTIVITY_PAGE_SIZE = 30
