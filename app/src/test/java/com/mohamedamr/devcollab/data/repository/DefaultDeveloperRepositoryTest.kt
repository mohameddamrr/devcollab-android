package com.mohamedamr.devcollab.data.repository

import com.mohamedamr.devcollab.data.github.remote.GitHubDataSource
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubSearchResponseDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserDetailDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserSummaryDto
import com.mohamedamr.devcollab.data.github.remote.model.GitHubApiResult
import com.mohamedamr.devcollab.data.github.remote.model.GitHubRateLimit
import com.mohamedamr.devcollab.data.github.remote.model.GitHubRemoteError
import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultDeveloperRepositoryTest {
    @Test
    fun `successful search trims query and maps DTOs to domain models`() = runTest {
        val dataSource = FakeGitHubDataSource(
            searchResult = GitHubApiResult.Success(
                data = GitHubSearchResponseDto(
                    totalCount = 1,
                    incompleteResults = false,
                    items = listOf(
                        GitHubUserSummaryDto(
                            login = "octocat",
                            id = 1L,
                            avatarUrl = "https://example.com/avatar.png",
                            htmlUrl = "https://github.com/octocat",
                            type = "User",
                            isSiteAdmin = false,
                            score = 99.0,
                        ),
                    ),
                ),
                rateLimit = GitHubRateLimit(),
            ),
        )
        val repository = DefaultDeveloperRepository(dataSource)

        val result = repository.searchDevelopers("  octocat  ", page = 2, pageSize = 20)

        assertEquals("octocat", dataSource.receivedQuery)
        assertEquals(2, dataSource.receivedPage)
        assertEquals(20, dataSource.receivedPageSize)
        assertTrue(result is DeveloperRepositoryResult.Success)
        val page = (result as DeveloperRepositoryResult.Success).data
        assertEquals(1, page.totalCount)
        assertEquals(DeveloperAccountType.User, page.developers.single().accountType)
        assertEquals(1L, page.developers.single().githubId)
    }

    @Test
    fun `rate limit error is translated without exposing GitHub error type`() = runTest {
        val dataSource = FakeGitHubDataSource(
            searchResult = GitHubApiResult.Failure(
                GitHubRemoteError.RateLimited(
                    statusCode = 403,
                    message = "rate limit exceeded",
                    rateLimit = GitHubRateLimit(resetAtEpochSeconds = 1234L),
                ),
            ),
        )
        val repository = DefaultDeveloperRepository(dataSource)

        val result = repository.searchDevelopers("android")

        assertEquals(
            DeveloperRepositoryResult.Failure(
                DeveloperRepositoryError.RateLimited(resetAtEpochSeconds = 1234L),
            ),
            result,
        )
    }

    @Test
    fun `unknown GitHub account type maps to Unknown`() = runTest {
        val dataSource = FakeGitHubDataSource(
            searchResult = GitHubApiResult.Success(
                data = GitHubSearchResponseDto(
                    totalCount = 1,
                    incompleteResults = false,
                    items = listOf(
                        GitHubUserSummaryDto(
                            login = "future-type",
                            id = 2L,
                            avatarUrl = "avatar",
                            htmlUrl = "profile",
                            type = "FutureType",
                            isSiteAdmin = false,
                            score = 1.0,
                        ),
                    ),
                ),
                rateLimit = GitHubRateLimit(),
            ),
        )

        val result = DefaultDeveloperRepository(dataSource).searchDevelopers("future")

        val page = (result as DeveloperRepositoryResult.Success).data
        assertEquals(DeveloperAccountType.Unknown, page.developers.single().accountType)
    }
}

private class FakeGitHubDataSource(
    private val searchResult: GitHubApiResult<GitHubSearchResponseDto>,
) : GitHubDataSource {
    var receivedQuery: String? = null
    var receivedPage: Int? = null
    var receivedPageSize: Int? = null

    override suspend fun searchUsers(
        query: String,
        page: Int,
        perPage: Int,
    ): GitHubApiResult<GitHubSearchResponseDto> {
        receivedQuery = query
        receivedPage = page
        receivedPageSize = perPage
        return searchResult
    }

    override suspend fun getUser(username: String): GitHubApiResult<GitHubUserDetailDto> =
        error("Not needed by these tests")
}
