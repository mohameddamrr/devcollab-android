package com.mohamedamr.devcollab.feature.developerdetails

import androidx.paging.PagingData
import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import com.mohamedamr.devcollab.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
    }
}

private class FakeDetailsRepository(
    private val resultProvider: suspend () -> DeveloperRepositoryResult<DeveloperProfile> = {
        error("No details result configured")
    },
) : DeveloperRepository {
    override fun getPagedDevelopers(query: String): Flow<PagingData<DeveloperSummary>> =
        flowOf(PagingData.empty())

    var callCount = 0
    var receivedUsername: String? = null

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
}
