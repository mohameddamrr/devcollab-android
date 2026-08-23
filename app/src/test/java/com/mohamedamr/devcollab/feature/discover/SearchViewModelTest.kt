package com.mohamedamr.devcollab.feature.discover

import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import com.mohamedamr.devcollab.testutil.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state contains an empty query and initial result`() {
        val viewModel = SearchViewModel(FakeDeveloperRepository())

        assertEquals(SearchUiState(), viewModel.uiState.value)
    }

    @Test
    fun `blank search displays validation error without calling repository`() {
        val repository = FakeDeveloperRepository()
        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChanged("   ")

        viewModel.search()

        assertEquals(
            SearchResultUiState.Error(SearchErrorReason.EmptyQuery),
            viewModel.uiState.value.result,
        )
        assertEquals(0, repository.callCount)
    }

    @Test
    fun `search emits loading then success`() = runTest {
        val pendingResult = CompletableDeferred<DeveloperRepositoryResult<DeveloperSearchPage>>()
        val repository = FakeDeveloperRepository { pendingResult.await() }
        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChanged("  octocat  ")

        viewModel.search()
        runCurrent()

        assertEquals("octocat", viewModel.uiState.value.query)
        assertEquals(SearchResultUiState.Loading, viewModel.uiState.value.result)

        pendingResult.complete(
            DeveloperRepositoryResult.Success(
                DeveloperSearchPage(
                    developers = listOf(testDeveloper),
                    totalCount = 1,
                    isIncomplete = false,
                ),
            ),
        )
        runCurrent()

        assertEquals(
            SearchResultUiState.Success(
                developers = listOf(testDeveloper),
                totalCount = 1,
            ),
            viewModel.uiState.value.result,
        )
    }

    @Test
    fun `successful search with no developers emits empty state`() = runTest {
        val repository = FakeDeveloperRepository {
            DeveloperRepositoryResult.Success(
                DeveloperSearchPage(
                    developers = emptyList(),
                    totalCount = 0,
                    isIncomplete = false,
                ),
            )
        }
        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChanged("nobody-has-this-login")

        viewModel.search()
        runCurrent()

        assertEquals(SearchResultUiState.Empty, viewModel.uiState.value.result)
    }

    @Test
    fun `network failure emits network error state`() = runTest {
        val repository = FakeDeveloperRepository {
            DeveloperRepositoryResult.Failure(DeveloperRepositoryError.NetworkUnavailable)
        }
        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChanged("android")

        viewModel.search()
        runCurrent()

        assertEquals(
            SearchResultUiState.Error(SearchErrorReason.NetworkUnavailable),
            viewModel.uiState.value.result,
        )
    }

    @Test
    fun `unexpected repository exception emits unexpected error state`() = runTest {
        val repository = FakeDeveloperRepository { error("mapper failed") }
        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChanged("android")

        viewModel.search()
        runCurrent()

        assertEquals(
            SearchResultUiState.Error(SearchErrorReason.Unexpected),
            viewModel.uiState.value.result,
        )
    }

    @Test
    fun `duplicate submit while same search is loading does not restart request`() = runTest {
        val pendingResult = CompletableDeferred<DeveloperRepositoryResult<DeveloperSearchPage>>()
        val repository = FakeDeveloperRepository { pendingResult.await() }
        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChanged("android")
        viewModel.search()
        runCurrent()

        viewModel.search()
        runCurrent()

        assertEquals(1, repository.callCount)
        assertEquals(SearchResultUiState.Loading, viewModel.uiState.value.result)
    }

    @Test
    fun `starting another search cancels the previous request and second result wins`() = runTest {
        val firstResult = CompletableDeferred<DeveloperRepositoryResult<DeveloperSearchPage>>()
        val secondResult = CompletableDeferred<DeveloperRepositoryResult<DeveloperSearchPage>>()
        val repository = FakeDeveloperRepository { query ->
            when (query) {
                "first" -> firstResult.await()
                "second" -> secondResult.await()
                else -> error("Unexpected query: $query")
            }
        }
        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChanged("first")
        viewModel.search()
        runCurrent()

        viewModel.onQueryChanged("second")
        viewModel.search()
        runCurrent()

        assertEquals(2, repository.callCount)
        assertEquals("second", repository.lastQuery)
        assertTrue("first" in repository.cancelledQueries)

        secondResult.complete(
            DeveloperRepositoryResult.Success(
                DeveloperSearchPage(
                    developers = listOf(testDeveloper.copy(login = "second")),
                    totalCount = 1,
                    isIncomplete = false,
                ),
            ),
        )
        runCurrent()

        val success = viewModel.uiState.value.result as SearchResultUiState.Success
        assertEquals("second", success.developers.single().login)
    }

    @Test
    fun `clearing query cancels pending search and keeps initial state`() = runTest {
        val pendingResult = CompletableDeferred<DeveloperRepositoryResult<DeveloperSearchPage>>()
        val repository = FakeDeveloperRepository { pendingResult.await() }
        val viewModel = SearchViewModel(repository)
        viewModel.onQueryChanged("android")
        viewModel.search()
        runCurrent()

        viewModel.onQueryChanged("")
        runCurrent()

        assertTrue("android" in repository.cancelledQueries)
        assertEquals("", viewModel.uiState.value.query)
        assertEquals(SearchResultUiState.Initial, viewModel.uiState.value.result)
    }

    private companion object {
        val testDeveloper = DeveloperSummary(
            githubId = 1L,
            login = "octocat",
            avatarUrl = "avatar",
            profileUrl = "profile",
            accountType = DeveloperAccountType.User,
            isSiteAdmin = false,
        )
    }
}

private class FakeDeveloperRepository(
    private val resultProvider: suspend (String) -> DeveloperRepositoryResult<DeveloperSearchPage> = {
        DeveloperRepositoryResult.Success(
            DeveloperSearchPage(emptyList(), totalCount = 0, isIncomplete = false),
        )
    },
) : DeveloperRepository {
    var callCount = 0
    var lastQuery: String? = null
    val cancelledQueries = mutableListOf<String>()

    override suspend fun searchDevelopers(
        query: String,
        page: Int,
        pageSize: Int,
    ): DeveloperRepositoryResult<DeveloperSearchPage> {
        callCount += 1
        lastQuery = query
        return try {
            resultProvider(query)
        } finally {
            if (!currentCoroutineContext().isActive) {
                cancelledQueries += query
            }
        }
    }
}
