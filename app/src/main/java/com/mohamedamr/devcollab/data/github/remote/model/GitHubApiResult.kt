package com.mohamedamr.devcollab.data.github.remote.model

sealed interface GitHubApiResult<out T> {
    data class Success<T>(
        val data: T,
        val rateLimit: GitHubRateLimit,
    ) : GitHubApiResult<T>

    data class Failure(
        val error: GitHubRemoteError,
    ) : GitHubApiResult<Nothing>
}
