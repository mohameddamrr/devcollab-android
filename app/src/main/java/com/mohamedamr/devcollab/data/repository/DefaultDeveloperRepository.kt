package com.mohamedamr.devcollab.data.repository

import com.mohamedamr.devcollab.data.github.mapper.toDomain
import com.mohamedamr.devcollab.data.github.remote.GitHubDataSource
import com.mohamedamr.devcollab.data.github.remote.model.GitHubApiResult
import com.mohamedamr.devcollab.data.github.remote.model.GitHubRemoteError
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult

class DefaultDeveloperRepository(
    private val remoteDataSource: GitHubDataSource,
) : DeveloperRepository {
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
