package com.mohamedamr.devcollab.data.github.remote

import com.mohamedamr.devcollab.data.github.remote.model.GitHubApiResult
import com.mohamedamr.devcollab.data.github.remote.model.GitHubRemoteError
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubSearchResponseDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserDetailDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubRepositoryDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubEventDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class GitHubRemoteDataSourceTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `search sends expected request and parses rate limit metadata`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("X-RateLimit-Limit", "10")
                .setHeader("X-RateLimit-Remaining", "9")
                .setHeader("X-RateLimit-Used", "1")
                .setHeader("X-RateLimit-Reset", "1_800_000_000".replace("_", ""))
                .setHeader("X-RateLimit-Resource", "search")
                .setBody(SEARCH_RESPONSE_JSON),
        )
        val dataSource = createDataSource(accessToken = "test-token")

        val result = dataSource.searchUsers(
            query = "kotlin android",
            page = 2,
            perPage = 20,
        )

        assertTrue(result is GitHubApiResult.Success)
        val success = result as GitHubApiResult.Success
        assertEquals(1, success.data.items.size)
        assertEquals("octocat", success.data.items.single().login)
        assertEquals(9, success.rateLimit.remaining)
        assertEquals("search", success.rateLimit.resource)

        val request = server.takeRequest()
        assertEquals("/search/users", request.requestUrl?.encodedPath)
        assertEquals("kotlin android", request.requestUrl?.queryParameter("q"))
        assertEquals("2", request.requestUrl?.queryParameter("page"))
        assertEquals("20", request.requestUrl?.queryParameter("per_page"))
        assertEquals(
            GitHubRequestInterceptor.GITHUB_JSON_MEDIA_TYPE,
            request.getHeader(GitHubRequestInterceptor.ACCEPT_HEADER),
        )
        assertEquals(
            GitHubRequestInterceptor.API_VERSION,
            request.getHeader(GitHubRequestInterceptor.API_VERSION_HEADER),
        )
        assertEquals("Bearer test-token", request.getHeader("Authorization"))
    }

    @Test
    fun `user details preserve unavailable optional fields as null`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(USER_DETAIL_RESPONSE_JSON),
        )
        val dataSource = createDataSource()

        val result = dataSource.getUser("octocat")

        assertTrue(result is GitHubApiResult.Success)
        val user = (result as GitHubApiResult.Success).data
        assertEquals(1L, user.id)
        assertNull(user.company)
        assertNull(user.location)
        assertFalse(user.isSiteAdmin)
        assertEquals("/users/octocat", server.takeRequest().path)
    }

    @Test
    fun `repositories request uses bounded paging and parses optional fields`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(REPOSITORIES_RESPONSE_JSON),
        )
        val dataSource = createDataSource()

        val result = dataSource.getUserRepositories(
            username = "octocat",
            page = 2,
            perPage = 20,
        )

        assertTrue(result is GitHubApiResult.Success)
        val repository = (result as GitHubApiResult.Success).data.single()
        assertEquals(1296269L, repository.id)
        assertNull(repository.description)
        assertEquals("Kotlin", repository.language)

        val request = server.takeRequest()
        assertEquals("/users/octocat/repos", request.requestUrl?.encodedPath)
        assertEquals("owner", request.requestUrl?.queryParameter("type"))
        assertEquals("updated", request.requestUrl?.queryParameter("sort"))
        assertEquals("desc", request.requestUrl?.queryParameter("direction"))
        assertEquals("2", request.requestUrl?.queryParameter("page"))
        assertEquals("20", request.requestUrl?.queryParameter("per_page"))
    }

    @Test
    fun `public events request is bounded and preserves event payload evidence`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(EVENTS_RESPONSE_JSON),
        )
        val dataSource = createDataSource()

        val result = dataSource.getUserPublicEvents("octocat", page = 1, perPage = 30)

        assertTrue(result is GitHubApiResult.Success)
        val event = (result as GitHubApiResult.Success).data.single()
        assertEquals("PushEvent", event.type)
        assertEquals("octocat/Hello-World", event.repo.name)
        assertEquals("2", event.payload["size"]?.toString())
        val request = server.takeRequest()
        assertEquals("/users/octocat/events/public", request.requestUrl?.encodedPath)
        assertEquals("1", request.requestUrl?.queryParameter("page"))
        assertEquals("30", request.requestUrl?.queryParameter("per_page"))
    }

    @Test
    fun `invalid repository page size is rejected before request`() {
        val dataSource = createDataSource()

        val error = runCatching {
            runBlocking {
                dataSource.getUserRepositories("octocat", page = 1, perPage = 101)
            }
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `exhausted limit returns typed rate limited error`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setHeader("X-RateLimit-Limit", "10")
                .setHeader("X-RateLimit-Remaining", "0")
                .setHeader("X-RateLimit-Reset", "1800000000")
                .setBody(ERROR_RESPONSE_JSON),
        )
        val dataSource = createDataSource()

        val result = dataSource.searchUsers("android")

        assertTrue(result is GitHubApiResult.Failure)
        val error = (result as GitHubApiResult.Failure).error
        assertTrue(error is GitHubRemoteError.RateLimited)
        error as GitHubRemoteError.RateLimited
        assertEquals(403, error.statusCode)
        assertEquals("API rate limit exceeded", error.message)
        assertEquals(1_800_000_000L, error.rateLimit.resetAtEpochSeconds)
    }

    @Test
    fun `blank search query is rejected before an HTTP request`() {
        val dataSource = createDataSource()

        val error = runCatching {
            runBlocking { dataSource.searchUsers("   ") }
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `ordinary not found remains an HTTP error even when remaining is zero`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setHeader("X-RateLimit-Remaining", "0")
                .setBody(NOT_FOUND_RESPONSE_JSON),
        )
        val dataSource = createDataSource()

        val result = dataSource.getUser("missing-user")

        val error = (result as GitHubApiResult.Failure).error
        assertTrue(error is GitHubRemoteError.Http)
        error as GitHubRemoteError.Http
        assertEquals(404, error.statusCode)
        assertEquals("Not Found", error.message)
    }

    @Test
    fun `secondary rate limit message returns typed rate limited error`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setHeader("X-RateLimit-Remaining", "8")
                .setBody(SECONDARY_RATE_LIMIT_RESPONSE_JSON),
        )
        val dataSource = createDataSource()

        val result = dataSource.searchUsers("android")

        val error = (result as GitHubApiResult.Failure).error
        assertTrue(error is GitHubRemoteError.RateLimited)
        assertEquals(
            "You have exceeded a secondary rate limit.",
            (error as GitHubRemoteError.RateLimited).message,
        )
    }

    @Test
    fun `too many requests preserves retry after metadata`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setHeader("Content-Type", "application/json")
                .setHeader("Retry-After", "60")
                .setBody(ERROR_RESPONSE_JSON),
        )
        val dataSource = createDataSource()

        val result = dataSource.searchUsers("android")

        val error = (result as GitHubApiResult.Failure).error
        assertTrue(error is GitHubRemoteError.RateLimited)
        assertEquals(60L, (error as GitHubRemoteError.RateLimited).rateLimit.retryAfterSeconds)
    }

    @Test
    fun `malformed successful JSON returns invalid response`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ invalid-json"),
        )
        val dataSource = createDataSource()

        val result = dataSource.searchUsers("android")

        val error = (result as GitHubApiResult.Failure).error
        assertTrue(error is GitHubRemoteError.InvalidResponse)
    }

    @Test
    fun `blank token is omitted from authorization header`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(SEARCH_RESPONSE_JSON),
        )
        val dataSource = createDataSource(accessToken = "   ")

        dataSource.searchUsers("android")

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `coroutine cancellation is rethrown`() {
        val cancellingService = object : GitHubApiService {
            override suspend fun searchUsers(
                query: String,
                page: Int,
                perPage: Int,
            ): Response<GitHubSearchResponseDto> = throw CancellationException("cancelled")

            override suspend fun getUser(
                username: String,
            ): Response<GitHubUserDetailDto> = throw CancellationException("cancelled")

            override suspend fun getUserRepositories(
                username: String,
                type: String,
                sort: String,
                direction: String,
                page: Int,
                perPage: Int,
            ): Response<List<GitHubRepositoryDto>> = throw CancellationException("cancelled")

            override suspend fun getUserPublicEvents(
                username: String,
                page: Int,
                perPage: Int,
            ): Response<List<GitHubEventDto>> = throw CancellationException("cancelled")

            override suspend fun searchRepositories(query: String, sort: String, order: String, page: Int, perPage: Int) =
                throw CancellationException("cancelled")

            override suspend fun getRepository(owner: String, repository: String): Response<GitHubRepositoryDto> =
                throw CancellationException("cancelled")

            override suspend fun getRepositoryContributors(owner: String, repository: String, includeAnonymous: Boolean, page: Int, perPage: Int) =
                throw CancellationException("cancelled")
        }
        val dataSource = GitHubRemoteDataSource(cancellingService)

        val error = runCatching {
            runBlocking { dataSource.searchUsers("android") }
        }.exceptionOrNull()

        assertTrue(error is CancellationException)
    }

    private fun createDataSource(accessToken: String? = null): GitHubRemoteDataSource {
        val apiService = GitHubClientFactory.create(
            isDebugBuild = false,
            accessTokenProvider = { accessToken },
            baseUrl = server.url("/").toString(),
        )
        return GitHubRemoteDataSource(apiService)
    }

    companion object {
        private val SEARCH_RESPONSE_JSON = """
            {
              "total_count": 1,
              "incomplete_results": false,
              "items": [
                {
                  "login": "octocat",
                  "id": 1,
                  "avatar_url": "https://avatars.example/octocat",
                  "html_url": "https://github.com/octocat",
                  "type": "User",
                  "site_admin": false,
                  "score": 1.0,
                  "unknown_future_field": "ignored"
                }
              ]
            }
        """.trimIndent()

        private val USER_DETAIL_RESPONSE_JSON = """
            {
              "login": "octocat",
              "id": 1,
              "avatar_url": "https://avatars.example/octocat",
              "html_url": "https://github.com/octocat",
              "type": "User",
              "site_admin": false,
              "name": "The Octocat",
              "company": null,
              "blog": "https://github.blog",
              "location": null,
              "email": null,
              "hireable": null,
              "bio": "GitHub mascot",
              "twitter_username": null,
              "public_repos": 8,
              "public_gists": 8,
              "followers": 100,
              "following": 2,
              "created_at": "2011-01-25T18:44:36Z",
              "updated_at": "2026-01-01T00:00:00Z"
            }
        """.trimIndent()

        private val REPOSITORIES_RESPONSE_JSON = """
            [
              {
                "id": 1296269,
                "name": "Hello-World",
                "full_name": "octocat/Hello-World",
                "html_url": "https://github.com/octocat/Hello-World",
                "description": null,
                "language": "Kotlin",
                "stargazers_count": 80,
                "forks_count": 9,
                "open_issues_count": 2,
                "fork": false,
                "archived": false,
                "disabled": false,
                "updated_at": "2026-08-20T10:00:00Z",
                "pushed_at": null,
                "unknown_future_field": "ignored"
              }
            ]
        """.trimIndent()

        private val EVENTS_RESPONSE_JSON = """
            [
              {
                "id": "event-1",
                "type": "PushEvent",
                "repo": { "id": 1, "name": "octocat/Hello-World" },
                "payload": { "size": 2 },
                "public": true,
                "created_at": "2026-08-24T10:00:00Z"
              }
            ]
        """.trimIndent()

        private val ERROR_RESPONSE_JSON = """
            {
              "message": "API rate limit exceeded",
              "documentation_url": "https://docs.github.com/rest/using-the-rest-api/rate-limits-for-the-rest-api",
              "status": "403"
            }
        """.trimIndent()

        private val NOT_FOUND_RESPONSE_JSON = """
            {
              "message": "Not Found",
              "documentation_url": "https://docs.github.com/rest/users/users#get-a-user",
              "status": "404"
            }
        """.trimIndent()

        private val SECONDARY_RATE_LIMIT_RESPONSE_JSON = """
            {
              "message": "You have exceeded a secondary rate limit.",
              "documentation_url": "https://docs.github.com/rest/using-the-rest-api/rate-limits-for-the-rest-api",
              "status": "403"
            }
        """.trimIndent()
    }
}
