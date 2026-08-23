package com.mohamedamr.devcollab.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.mohamedamr.devcollab.data.github.mapper.toDomain
import com.mohamedamr.devcollab.data.github.remote.GitHubDataSource
import com.mohamedamr.devcollab.data.github.remote.model.GitHubApiResult
import com.mohamedamr.devcollab.data.github.remote.model.GitHubRemoteError
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import kotlinx.coroutines.flow.Flow

class DefaultDeveloperRepository(
    private val remoteDataSource: GitHubDataSource,
) : DeveloperRepository {
    override fun getPagedDevelopers(query: String): Flow<PagingData<DeveloperSummary>> = Pager(
        config = PagingConfig(
            pageSize = SEARCH_PAGE_SIZE,
            initialLoadSize = SEARCH_PAGE_SIZE,
            enablePlaceholders = false,
            prefetchDistance = SEARCH_PREFETCH_DISTANCE,
        ),
        pagingSourceFactory = {
            val normalizedQuery = query.trim()
            require(normalizedQuery.isNotEmpty()) { "Paging search query must not be blank." }
            DeveloperSearchPagingSource { page, pageSize ->
                searchDevelopers(
                    query = normalizedQuery,
                    page = page,
                    pageSize = pageSize,
                )
            }
        },
    ).flow

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
