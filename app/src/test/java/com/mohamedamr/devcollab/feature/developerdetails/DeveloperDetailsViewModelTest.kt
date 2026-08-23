package com.mohamedamr.devcollab.feature.developerdetails

import androidx.paging.PagingData
import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.model.DeveloperRepositorySummary
import com.mohamedamr.devcollab.domain.model.DeveloperActivity
import com.mohamedamr.devcollab.domain.model.LastSearch
import com.mohamedamr.devcollab.domain.model.SearchDataStatus
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import com.mohamedamr.devcollab.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeveloperDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial load trims username and emits success`() = runTest {
        val repository = FakeDetailsRepository {
            DeveloperRepositoryResult.Success(testProfile)
        }

        val viewModel = DeveloperDetailsViewModel("  octocat  ", repository)
        runCurrent()

        assertEquals("octocat", repository.receivedUsername)
        assertEquals(
            DeveloperDetailsResultUiState.Success(testProfile),
            viewModel.uiState.value.result,
        )
    }

    @Test
    fun `blank username displays invalid username without repository call`() = runTest {
        val repository = FakeDetailsRepository()

        val viewModel = DeveloperDetailsViewModel("   ", repository)
        runCurrent()

        assertEquals(0, repository.callCount)
        assertEquals(
            DeveloperDetailsResultUiState.Error(
                DeveloperDetailsErrorReason.InvalidUsername,
            ),
            viewModel.uiState.value.result,
        )
    }

    @Test
    fun `404 response displays not found error`() = runTest {
        val repository = FakeDetailsRepository {
            DeveloperRepositoryResult.Failure(
                DeveloperRepositoryError.Server(statusCode = 404, message = "Not Found"),
            )
        }

        val viewModel = DeveloperDetailsViewModel("missing", repository)
        runCurrent()

        assertEquals(
            DeveloperDetailsResultUiState.Error(DeveloperDetailsErrorReason.NotFound),
            viewModel.uiState.value.result,
        )
    }

    @Test
    fun `network failure displays network error`() = runTest {
        val repository = FakeDetailsRepository {
            DeveloperRepositoryResult.Failure(DeveloperRepositoryError.NetworkUnavailable)
        }

        val viewModel = DeveloperDetailsViewModel("octocat", repository)
        runCurrent()

        assertEquals(
            DeveloperDetailsResultUiState.Error(
                DeveloperDetailsErrorReason.NetworkUnavailable,
            ),
            viewModel.uiState.value.result,
        )
    }

    @Test
    fun `retry loads profile after failure`() = runTest {
        var shouldSucceed = false
        val repository = FakeDetailsRepository {
            if (shouldSucceed) {
                DeveloperRepositoryResult.Success(testProfile)
            } else {
                DeveloperRepositoryResult.Failure(DeveloperRepositoryError.NetworkUnavailable)
            }
        }
        val viewModel = DeveloperDetailsViewModel("octocat", repository)
        runCurrent()
        shouldSucceed = true

        viewModel.retry()
        runCurrent()

        assertEquals(2, repository.callCount)
        assertEquals(
            DeveloperDetailsResultUiState.Success(testProfile),
            viewModel.uiState.value.result,
        )
    }

    @Test
    fun `duplicate retry while loading does not duplicate request`() = runTest {
        val pendingResult = CompletableDeferred<DeveloperRepositoryResult<DeveloperProfile>>()
        val repository = FakeDetailsRepository { pendingResult.await() }
        val viewModel = DeveloperDetailsViewModel("octocat", repository)
        runCurrent()

        viewModel.retry()
        runCurrent()

        assertEquals(1, repository.callCount)
        assertEquals(DeveloperDetailsResultUiState.Loading, viewModel.uiState.value.result)
    }

    @Test
    fun `unexpected repository exception displays unexpected error`() = runTest {
        val repository = FakeDetailsRepository { error("mapping failed") }

        val viewModel = DeveloperDetailsViewModel("octocat", repository)
        runCurrent()

        assertEquals(
            DeveloperDetailsResultUiState.Error(DeveloperDetailsErrorReason.Unexpected),
            viewModel.uiState.value.result,
        )
    }

    @Test
    fun `repository failure does not replace successful profile`() = runTest {
        val repository = FakeDetailsRepository(
            repositoryResultProvider = {
                DeveloperRepositoryResult.Failure(DeveloperRepositoryError.NetworkUnavailable)
            },
            resultProvider = { DeveloperRepositoryResult.Success(testProfile) },
        )

        val viewModel = DeveloperDetailsViewModel("octocat", repository)
        runCurrent()

        assertEquals(
            DeveloperDetailsResultUiState.Success(testProfile),
            viewModel.uiState.value.result,
        )
        assertEquals(
            DeveloperRepositoriesUiState.Error(
                DeveloperDetailsErrorReason.NetworkUnavailable,
            ),
            viewModel.uiState.value.repositories,
        )
    }

    @Test
    fun `repository success exposes recently updated page and tab selection`() = runTest {
        val repositories = listOf(testRepository)
        val repository = FakeDetailsRepository(
            repositoryResultProvider = {
                DeveloperRepositoryResult.Success(repositories)
            },
            resultProvider = { DeveloperRepositoryResult.Success(testProfile) },
        )
        val viewModel = DeveloperDetailsViewModel("octocat", repository)
        runCurrent()

        viewModel.selectTab(DeveloperProfileTab.Repositories)

        assertEquals(DeveloperProfileTab.Repositories, viewModel.uiState.value.selectedTab)
        assertEquals(
            DeveloperRepositoriesUiState.Success(repositories, canLoadMore = false),
            viewModel.uiState.value.repositories,
        )
        assertEquals("octocat", repository.receivedRepositoryUsername)
        assertEquals(1, repository.receivedRepositoryPage)
        assertEquals(30, repository.receivedRepositoryPageSize)
    }

    @Test
    fun `loading more appends and deduplicates repository pages`() = runTest {
        val firstPage = List(30) { index ->
            testRepository.copy(githubId = index.toLong(), name = "repo-$index")
        }
        val repository = FakeDetailsRepository(
            repositoryResultProvider = { page ->
                DeveloperRepositoryResult.Success(
                    if (page == 1) {
                        firstPage
                    } else {
                        listOf(
                            firstPage.last(),
                            testRepository.copy(githubId = 30L, name = "repo-30"),
                        )
                    },
                )
            },
            resultProvider = { DeveloperRepositoryResult.Success(testProfile) },
        )
        val viewModel = DeveloperDetailsViewModel("octocat", repository)
        runCurrent()

        viewModel.loadMoreRepositories()
        runCurrent()

        val state = viewModel.uiState.value.repositories as DeveloperRepositoriesUiState.Success
        assertEquals(31, state.repositories.size)
        assertEquals(31, state.repositories.map { it.githubId }.distinct().size)
        assertEquals(false, state.canLoadMore)
        assertEquals(listOf(1, 2), repository.receivedRepositoryPages)
    }

    @Test
    fun `append failure keeps loaded repositories and exposes retryable append error`() = runTest {
        val firstPage = List(30) { index ->
            testRepository.copy(githubId = index.toLong(), name = "repo-$index")
        }
        val repository = FakeDetailsRepository(
            repositoryResultProvider = { page ->
                if (page == 1) {
                    DeveloperRepositoryResult.Success(firstPage)
                } else {
                    DeveloperRepositoryResult.Failure(
                        DeveloperRepositoryError.NetworkUnavailable,
                    )
                }
            },
            resultProvider = { DeveloperRepositoryResult.Success(testProfile) },
        )
        val viewModel = DeveloperDetailsViewModel("octocat", repository)
        runCurrent()

        viewModel.loadMoreRepositories()
        runCurrent()

        val state = viewModel.uiState.value.repositories as DeveloperRepositoriesUiState.Success
        assertEquals(firstPage, state.repositories)
        assertEquals(
            DeveloperDetailsErrorReason.NetworkUnavailable,
            state.appendError,
        )
    }

    @Test
    fun `failed refresh preserves profile and repositories and exposes refresh error`() = runTest {
        var refreshShouldFail = false
        val repositories = listOf(testRepository)
        val repository = FakeDetailsRepository(
            repositoryResultProvider = {
                if (refreshShouldFail) {
                    DeveloperRepositoryResult.Failure(
                        DeveloperRepositoryError.NetworkUnavailable,
                    )
                } else {
                    DeveloperRepositoryResult.Success(repositories)
                }
            },
            resultProvider = {
                if (refreshShouldFail) {
                    DeveloperRepositoryResult.Failure(
                        DeveloperRepositoryError.NetworkUnavailable,
                    )
                } else {
                    DeveloperRepositoryResult.Success(testProfile)
                }
            },
        )
        val viewModel = DeveloperDetailsViewModel("octocat", repository)
        runCurrent()
        refreshShouldFail = true

        viewModel.refresh()
        runCurrent()

        assertEquals(
            DeveloperDetailsResultUiState.Success(testProfile),
            viewModel.uiState.value.result,
        )
        assertEquals(
            DeveloperRepositoriesUiState.Success(repositories, canLoadMore = false),
            viewModel.uiState.value.repositories,
        )
        assertEquals(
            DeveloperDetailsErrorReason.NetworkUnavailable,
            viewModel.uiState.value.refreshError,
        )
        assertEquals(false, viewModel.uiState.value.isRefreshing)
    }

    private companion object {
        val testProfile = DeveloperProfile(
            githubId = 1L,
            login = "octocat",
            avatarUrl = "avatar",
            profileUrl = "https://github.com/octocat",
            accountType = DeveloperAccountType.User,
            isSiteAdmin = false,
            name = "The Octocat",
            bio = "GitHub mascot",
            company = "GitHub",
            location = "San Francisco",
            websiteUrl = "https://github.blog",
            publicEmail = null,
            twitterUsername = null,
            isHireable = true,
            publicRepositoryCount = 8,
            publicGistCount = 2,
            followers = 20,
            following = 5,
            createdAt = "2011-01-25T18:44:36Z",
            updatedAt = "2026-01-01T00:00:00Z",
        )

        val testRepository = DeveloperRepositorySummary(
            githubId = 10L,
            name = "hello-compose",
            fullName = "octocat/hello-compose",
            repositoryUrl = "https://github.com/octocat/hello-compose",
            description = "Compose sample",
            primaryLanguage = "Kotlin",
            starCount = 12,
            forkCount = 3,
            openIssueCount = 1,
            isFork = false,
            isArchived = false,
            isDisabled = false,
            updatedAt = "2026-08-20T10:00:00Z",
            pushedAt = "2026-08-19T10:00:00Z",
        )
    }
}

private class FakeDetailsRepository(
    private val activityResultProvider: suspend () ->
        DeveloperRepositoryResult<List<DeveloperActivity>> = {
            DeveloperRepositoryResult.Success(emptyList())
        },
    private val repositoryResultProvider: suspend (page: Int) ->
        DeveloperRepositoryResult<List<DeveloperRepositorySummary>> = {
            DeveloperRepositoryResult.Success(emptyList())
        },
    private val resultProvider: suspend () -> DeveloperRepositoryResult<DeveloperProfile> = {
        error("No details result configured")
    },
) : DeveloperRepository {
    override val searchDataStatus: StateFlow<SearchDataStatus> =
        MutableStateFlow(SearchDataStatus.Unknown)

    override suspend fun getLastSearch(): LastSearch? = null

    override fun getPagedDevelopers(query: String): Flow<PagingData<DeveloperSummary>> =
        flowOf(PagingData.empty())

    var callCount = 0
    var receivedUsername: String? = null
    var receivedRepositoryUsername: String? = null
    var receivedRepositoryPage: Int? = null
    var receivedRepositoryPageSize: Int? = null
    val receivedRepositoryPages = mutableListOf<Int>()

    override suspend fun getDeveloperProfile(
        username: String,
    ): DeveloperRepositoryResult<DeveloperProfile> {
        callCount += 1
        receivedUsername = username
        return resultProvider()
    }

    override suspend fun searchDevelopers(
        query: String,
        page: Int,
        pageSize: Int,
    ): DeveloperRepositoryResult<DeveloperSearchPage> = error("Not needed by details tests")

    override suspend fun getDeveloperRepositories(
        username: String,
        page: Int,
        pageSize: Int,
    ): DeveloperRepositoryResult<List<DeveloperRepositorySummary>> {
        receivedRepositoryUsername = username
        receivedRepositoryPage = page
        receivedRepositoryPages += page
        receivedRepositoryPageSize = pageSize
        return repositoryResultProvider(page)
    }

    override suspend fun getDeveloperRecentActivity(
        username: String,
        pageSize: Int,
    ): DeveloperRepositoryResult<List<DeveloperActivity>> = activityResultProvider()
}
