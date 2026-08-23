package com.mohamedamr.devcollab.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.ExperimentalPagingApi
import androidx.paging.map
import com.mohamedamr.devcollab.data.github.mapper.toDomain
import com.mohamedamr.devcollab.data.github.remote.GitHubDataSource
import com.mohamedamr.devcollab.data.github.remote.model.GitHubApiResult
import com.mohamedamr.devcollab.data.github.remote.model.GitHubRemoteError
import com.mohamedamr.devcollab.data.local.dao.DeveloperSearchDao
import com.mohamedamr.devcollab.data.local.mapper.toDomain
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.model.LastSearch
import com.mohamedamr.devcollab.domain.model.SearchDataStatus
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalPagingApi::class)
class DefaultDeveloperRepository(
    private val remoteDataSource: GitHubDataSource,
    private val developerSearchDao: DeveloperSearchDao? = null,
) : DeveloperRepository {
    private val _searchDataStatus = MutableStateFlow<SearchDataStatus>(SearchDataStatus.Unknown)
    override val searchDataStatus: StateFlow<SearchDataStatus> = _searchDataStatus.asStateFlow()

    override suspend fun getLastSearch(): LastSearch? = developerSearchDao
        ?.getLastSearch()
        ?.let { entity ->
            LastSearch(
                query = entity.query,
                totalCount = entity.totalCount,
                lastSearchedAtEpochMillis = entity.lastSearchedAtEpochMillis,
            )
        }

    override fun getPagedDevelopers(query: String): Flow<PagingData<DeveloperSummary>> {
        val normalizedQuery = query.trim()
        require(normalizedQuery.isNotEmpty()) { "Paging search query must not be blank." }
        val dao = requireNotNull(developerSearchDao) {
            "A DeveloperSearchDao is required for paginated search."
        }
        _searchDataStatus.value = SearchDataStatus.Unknown

        return Pager(
            config = PagingConfig(
                pageSize = SEARCH_PAGE_SIZE,
                initialLoadSize = SEARCH_PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = SEARCH_PREFETCH_DISTANCE,
            ),
            remoteMediator = DeveloperSearchRemoteMediator(
                query = normalizedQuery,
                dao = dao,
                loadRemotePage = { page, pageSize ->
                    searchDevelopers(
                        query = normalizedQuery,
                        page = page,
                        pageSize = pageSize,
                    )
                },
                onDataStatusChanged = { status -> _searchDataStatus.value = status },
            ),
            pagingSourceFactory = { dao.pagingSource(normalizedQuery) },
        ).flow.map { pagingData -> pagingData.map { entity -> entity.toDomain() } }
    }

    override suspend fun searchDevelopers(
        query: String,
        page: Int,
        pageSize: Int,
    ): DeveloperRepositoryResult<DeveloperSearchPage> =
        when (
            val result = remoteDataSource.searchUsers(
                query = query.trim(),
                page = page,
                perPage = pageSize,
            )
        ) {
            is GitHubApiResult.Success -> DeveloperRepositoryResult.Success(result.data.toDomain())
            is GitHubApiResult.Failure -> DeveloperRepositoryResult.Failure(result.error.toDomain())
        }

    override suspend fun getDeveloperProfile(
        username: String,
    ): DeveloperRepositoryResult<DeveloperProfile> =
        when (val result = remoteDataSource.getUser(username.trim())) {
            is GitHubApiResult.Success -> DeveloperRepositoryResult.Success(result.data.toDomain())
            is GitHubApiResult.Failure -> DeveloperRepositoryResult.Failure(result.error.toDomain())
        }

    private companion object {
        const val SEARCH_PAGE_SIZE = 30
        const val SEARCH_PREFETCH_DISTANCE = 5
    }
}

private fun GitHubRemoteError.toDomain(): DeveloperRepositoryError = when (this) {
    is GitHubRemoteError.Network -> DeveloperRepositoryError.NetworkUnavailable
    is GitHubRemoteError.RateLimited -> DeveloperRepositoryError.RateLimited(
        resetAtEpochSeconds = rateLimit.resetAtEpochSeconds,
    )
    is GitHubRemoteError.Http -> DeveloperRepositoryError.Server(
        statusCode = statusCode,
        message = message,
    )
    is GitHubRemoteError.InvalidResponse -> DeveloperRepositoryError.InvalidData
    is GitHubRemoteError.Unexpected -> DeveloperRepositoryError.Unexpected
}
