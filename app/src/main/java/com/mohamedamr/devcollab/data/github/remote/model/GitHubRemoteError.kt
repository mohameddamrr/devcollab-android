package com.mohamedamr.devcollab.data.github.remote.model

import java.io.IOException

sealed interface GitHubRemoteError {
    data class Network(val cause: IOException) : GitHubRemoteError

    data class InvalidResponse(val cause: Throwable) : GitHubRemoteError

    data class RateLimited(
        val statusCode: Int,
        val message: String?,
        val rateLimit: GitHubRateLimit,
    ) : GitHubRemoteError

    data class Http(
        val statusCode: Int,
        val message: String?,
        val rateLimit: GitHubRateLimit,
    ) : GitHubRemoteError

    data class Unexpected(val cause: Throwable) : GitHubRemoteError
}
