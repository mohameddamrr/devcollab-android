package com.mohamedamr.devcollab.data.repository

import com.mohamedamr.devcollab.data.github.remote.GitHubDataSource
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubSearchResponseDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserDetailDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserSummaryDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubRepositoryDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubEventDto
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

    @Test
    fun `developer details trim username and map optional profile fields`() = runTest {
        val dataSource = FakeGitHubDataSource(
            searchResult = emptySearchResult,
            detailResult = GitHubApiResult.Success(
                data = testDetailDto.copy(
                    name = "  The Octocat  ",
                    company = "   ",
                    blog = " https://github.blog ",
                ),
                rateLimit = GitHubRateLimit(),
            ),
        )

        val result = DefaultDeveloperRepository(dataSource)
            .getDeveloperProfile("  octocat  ")

        assertEquals("octocat", dataSource.receivedUsername)
        val profile = (result as DeveloperRepositoryResult.Success).data
        assertEquals("The Octocat", profile.name)
        assertEquals(null, profile.company)
        assertEquals("https://github.blog", profile.websiteUrl)
        assertEquals(8, profile.publicRepositoryCount)
    }

    @Test
    fun `developer details translate server failure`() = runTest {
        val dataSource = FakeGitHubDataSource(
            searchResult = emptySearchResult,
            detailResult = GitHubApiResult.Failure(
                GitHubRemoteError.Http(
                    statusCode = 404,
                    message = "Not Found",
                    rateLimit = GitHubRateLimit(),
                ),
            ),
        )

        val result = DefaultDeveloperRepository(dataSource).getDeveloperProfile("missing")

        assertEquals(
            DeveloperRepositoryResult.Failure(
                DeveloperRepositoryError.Server(statusCode = 404, message = "Not Found"),
            ),
            result,
        )
    }

    @Test
    fun `developer repositories trim username and map GitHub evidence`() = runTest {
        val dataSource = FakeGitHubDataSource(
            searchResult = emptySearchResult,
            repositoryResult = GitHubApiResult.Success(
                data = listOf(testRepositoryDto.copy(description = "   ", language = " Kotlin ")),
                rateLimit = GitHubRateLimit(),
            ),
        )

        val result = DefaultDeveloperRepository(dataSource).getDeveloperRepositories(
            username = "  octocat  ",
            page = 2,
            pageSize = 20,
        )

        assertEquals("octocat", dataSource.receivedRepositoryUsername)
        assertEquals(2, dataSource.receivedRepositoryPage)
        assertEquals(20, dataSource.receivedRepositoryPageSize)
        val repository = (result as DeveloperRepositoryResult.Success).data.single()
        assertEquals(1296269L, repository.githubId)
        assertEquals(null, repository.description)
        assertEquals("Kotlin", repository.primaryLanguage)
        assertEquals(80, repository.starCount)
    }

    private companion object {
        val emptySearchResult = GitHubApiResult.Success(
            data = GitHubSearchResponseDto(
                totalCount = 0,
                incompleteResults = false,
                items = emptyList(),
            ),
            rateLimit = GitHubRateLimit(),
        )

        val testDetailDto = GitHubUserDetailDto(
            login = "octocat",
            id = 1L,
            avatarUrl = "avatar",
            htmlUrl = "https://github.com/octocat",
            type = "User",
            isSiteAdmin = false,
            name = "The Octocat",
            company = "GitHub",
            blog = "https://github.blog",
            location = "San Francisco",
            email = null,
            hireable = true,
            bio = "GitHub mascot",
            twitterUsername = null,
            publicRepositoryCount = 8,
            publicGistCount = 2,
            followers = 20,
            following = 5,
            createdAt = "2011-01-25T18:44:36Z",
            updatedAt = "2026-01-01T00:00:00Z",
        )

        val testRepositoryDto = GitHubRepositoryDto(
            id = 1296269L,
            name = "Hello-World",
            fullName = "octocat/Hello-World",
            htmlUrl = "https://github.com/octocat/Hello-World",
            description = "Example repository",
            language = "Kotlin",
            starCount = 80,
            forkCount = 9,
            openIssueCount = 2,
            fork = false,
            archived = false,
            disabled = false,
            updatedAt = "2026-08-20T10:00:00Z",
            pushedAt = null,
        )
    }
}

private class FakeGitHubDataSource(
    private val searchResult: GitHubApiResult<GitHubSearchResponseDto>,
    private val detailResult: GitHubApiResult<GitHubUserDetailDto>? = null,
    private val repositoryResult: GitHubApiResult<List<GitHubRepositoryDto>>? = null,
    private val activityResult: GitHubApiResult<List<GitHubEventDto>>? = null,
) : GitHubDataSource {
    var receivedQuery: String? = null
    var receivedPage: Int? = null
    var receivedPageSize: Int? = null
    var receivedUsername: String? = null
    var receivedRepositoryUsername: String? = null
    var receivedRepositoryPage: Int? = null
    var receivedRepositoryPageSize: Int? = null

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

    override suspend fun getUser(username: String): GitHubApiResult<GitHubUserDetailDto> {
        receivedUsername = username
        return checkNotNull(detailResult) { "No detail result configured for this test" }
    }

    override suspend fun getUserRepositories(
        username: String,
        page: Int,
        perPage: Int,
    ): GitHubApiResult<List<GitHubRepositoryDto>> {
        receivedRepositoryUsername = username
        receivedRepositoryPage = page
        receivedRepositoryPageSize = perPage
        return checkNotNull(repositoryResult) { "No repository result configured for this test" }
    }

    override suspend fun getUserPublicEvents(
        username: String,
        page: Int,
        perPage: Int,
    ): GitHubApiResult<List<GitHubEventDto>> =
        checkNotNull(activityResult) { "No activity result configured for this test" }

    override suspend fun searchRepositories(query: String, perPage: Int): GitHubApiResult<com.mohamedamr.devcollab.data.github.remote.dto.GitHubRepositorySearchResponseDto> =
        error("Not needed by these tests")

    override suspend fun getRepository(owner: String, repository: String): GitHubApiResult<GitHubRepositoryDto> =
        error("Not needed by these tests")

    override suspend fun getRepositoryContributors(owner: String, repository: String, perPage: Int): GitHubApiResult<List<com.mohamedamr.devcollab.data.github.remote.dto.GitHubContributorDto>> =
        error("Not needed by these tests")
}
