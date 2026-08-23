package com.mohamedamr.devcollab.feature.developerdetails

import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.model.DeveloperRepositorySummary
import com.mohamedamr.devcollab.domain.model.DeveloperActivity

data class DeveloperDetailsUiState(
    val username: String,
    val result: DeveloperDetailsResultUiState = DeveloperDetailsResultUiState.Loading,
    val selectedTab: DeveloperProfileTab = DeveloperProfileTab.Overview,
    val repositories: DeveloperRepositoriesUiState = DeveloperRepositoriesUiState.Loading,
    val repositoryQuery: String = "",
    val repositorySort: RepositorySort = RepositorySort.RecentlyUpdated,
    val isRefreshing: Boolean = false,
    val refreshError: DeveloperDetailsErrorReason? = null,
    val activity: DeveloperActivityUiState = DeveloperActivityUiState.Loading,
)

enum class DeveloperProfileTab {
    Overview,
    Repositories,
    Activity,
}

sealed interface DeveloperActivityUiState {
    data object Loading : DeveloperActivityUiState

    data class Success(
        val activities: List<DeveloperActivity>,
    ) : DeveloperActivityUiState

    data class Error(
        val reason: DeveloperDetailsErrorReason,
    ) : DeveloperActivityUiState
}

enum class RepositorySort {
    RecentlyUpdated,
    Popular,
    Name,
}

sealed interface DeveloperRepositoriesUiState {
    data object Loading : DeveloperRepositoriesUiState

    data class Success(
        val repositories: List<DeveloperRepositorySummary>,
        val canLoadMore: Boolean,
        val isLoadingMore: Boolean = false,
        val appendError: DeveloperDetailsErrorReason? = null,
    ) : DeveloperRepositoriesUiState

    data class Error(
        val reason: DeveloperDetailsErrorReason,
    ) : DeveloperRepositoriesUiState
}

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
