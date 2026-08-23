package com.mohamedamr.devcollab.data.github.remote

import com.mohamedamr.devcollab.data.github.remote.dto.GitHubErrorResponseDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubSearchResponseDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserDetailDto
import com.mohamedamr.devcollab.data.github.remote.model.GitHubApiResult
import com.mohamedamr.devcollab.data.github.remote.model.GitHubRateLimit
import com.mohamedamr.devcollab.data.github.remote.model.GitHubRemoteError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.Response
import java.io.IOException

class GitHubRemoteDataSource(
    private val apiService: GitHubApiService,
    private val json: Json = githubJson,
) : GitHubDataSource {
    override suspend fun searchUsers(
        query: String,
        page: Int,
        perPage: Int,
    ): GitHubApiResult<GitHubSearchResponseDto> {
        require(query.isNotBlank()) { "Search query must not be blank." }
        require(page >= GitHubApiService.DEFAULT_PAGE) { "Page must be at least 1." }
        require(perPage in 1..GitHubApiService.MAX_PAGE_SIZE) {
            "Page size must be between 1 and ${GitHubApiService.MAX_PAGE_SIZE}."
        }

        return executeRequest {
            apiService.searchUsers(
                query = query,
                page = page,
                perPage = perPage,
            )
        }
    }

    override suspend fun getUser(username: String): GitHubApiResult<GitHubUserDetailDto> {
        require(username.isNotBlank()) { "Username must not be blank." }
        return executeRequest { apiService.getUser(username) }
    }

    private suspend fun <T> executeRequest(
        request: suspend () -> Response<T>,
    ): GitHubApiResult<T> = try {
        request().toApiResult()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (networkError: IOException) {
        GitHubApiResult.Failure(GitHubRemoteError.Network(networkError))
    } catch (serializationError: SerializationException) {
        GitHubApiResult.Failure(GitHubRemoteError.InvalidResponse(serializationError))
    } catch (unexpected: Exception) {
        GitHubApiResult.Failure(GitHubRemoteError.Unexpected(unexpected))
    }

    private fun <T> Response<T>.toApiResult(): GitHubApiResult<T> {
        val rateLimit = headers().toRateLimit()
        val responseBody = body()

        if (isSuccessful) {
            return if (responseBody != null) {
                GitHubApiResult.Success(
                    data = responseBody,
                    rateLimit = rateLimit,
                )
            } else {
                GitHubApiResult.Failure(
                    GitHubRemoteError.InvalidResponse(
                        IllegalStateException("GitHub returned a successful response without a body."),
                    ),
                )
            }
        }

        val errorResponse = parseErrorResponse()
        val message = errorResponse?.message ?: message().takeIf(String::isNotBlank)
        val isRateLimited = isRateLimitResponse(
            statusCode = code(),
            errorResponse = errorResponse,
            rateLimit = rateLimit,
        )

        val error = if (isRateLimited) {
            GitHubRemoteError.RateLimited(
                statusCode = code(),
                message = message,
                rateLimit = rateLimit,
            )
        } else {
            GitHubRemoteError.Http(
                statusCode = code(),
                message = message,
                rateLimit = rateLimit,
            )
        }

        return GitHubApiResult.Failure(error)
    }

    private fun Response<*>.parseErrorResponse(): GitHubErrorResponseDto? {
        val errorJson = errorBody()?.string() ?: return null
        return runCatching {
            json.decodeFromString<GitHubErrorResponseDto>(errorJson)
        }.getOrNull()
    }

    private fun isRateLimitResponse(
        statusCode: Int,
        errorResponse: GitHubErrorResponseDto?,
        rateLimit: GitHubRateLimit,
    ): Boolean {
        if (statusCode == HTTP_TOO_MANY_REQUESTS) return true
        if (statusCode != HTTP_FORBIDDEN) return false

        val messageMentionsRateLimit = errorResponse?.message
            ?.contains(RATE_LIMIT_MESSAGE_MARKER, ignoreCase = true) == true
        val documentationMentionsRateLimit = errorResponse?.documentationUrl
            ?.contains(RATE_LIMIT_DOCUMENTATION_MARKER, ignoreCase = true) == true

        return rateLimit.remaining == 0 ||
            rateLimit.retryAfterSeconds != null ||
            messageMentionsRateLimit ||
            documentationMentionsRateLimit
    }

    private fun okhttp3.Headers.toRateLimit() = GitHubRateLimit(
        limit = this[RATE_LIMIT_LIMIT_HEADER]?.toIntOrNull(),
        remaining = this[RATE_LIMIT_REMAINING_HEADER]?.toIntOrNull(),
        used = this[RATE_LIMIT_USED_HEADER]?.toIntOrNull(),
        resetAtEpochSeconds = this[RATE_LIMIT_RESET_HEADER]?.toLongOrNull(),
        resource = this[RATE_LIMIT_RESOURCE_HEADER],
        retryAfterSeconds = this[RETRY_AFTER_HEADER]?.toLongOrNull(),
    )

    companion object {
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_FORBIDDEN = 403
        private const val RATE_LIMIT_MESSAGE_MARKER = "rate limit"
        private const val RATE_LIMIT_DOCUMENTATION_MARKER = "rate-limit"
        private const val RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit"
        private const val RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining"
        private const val RATE_LIMIT_USED_HEADER = "X-RateLimit-Used"
        private const val RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset"
        private const val RATE_LIMIT_RESOURCE_HEADER = "X-RateLimit-Resource"
        private const val RETRY_AFTER_HEADER = "Retry-After"
    }
}
