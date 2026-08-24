package com.mohamedamr.devcollab.feature.discover

import androidx.paging.PagingData
import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.model.DeveloperRepositorySummary
import com.mohamedamr.devcollab.domain.model.DeveloperActivity
import com.mohamedamr.devcollab.domain.model.LastSearch
import com.mohamedamr.devcollab.domain.model.SearchDataStatus
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import com.mohamedamr.devcollab.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `stored search is not restored when ViewModel starts`() = runTest {
        val repository = FakeDeveloperRepository(
            lastSearch = LastSearch(
                query = "kotlin",
                totalCount = 40,
                lastSearchedAtEpochMillis = 123L,
            ),
        )
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.pagingData.collect {}
        }

        runCurrent()

        assertEquals("", viewModel.uiState.value.query)
        assertFalse(viewModel.uiState.value.hasSubmittedSearch)
        assertEquals(emptyList<String>(), repository.pagedQueries)
    }

    @Test
    fun `cached data status exposes timestamp and fresh data clears it`() = runTest {
        val repository = FakeDeveloperRepository()
        val viewModel = SearchViewModel(repository)

        repository.dataStatus.value = SearchDataStatus.Cached(cachedAtEpochMillis = 456L)
        runCurrent()
        assertEquals(456L, viewModel.uiState.value.cachedAtEpochMillis)

        repository.dataStatus.value = SearchDataStatus.Fresh
        runCurrent()
        assertNull(viewModel.uiState.value.cachedAtEpochMillis)
    }

    @Test
    fun `blank search shows validation error without creating pager`() = runTest {
        val repository = FakeDeveloperRepository()
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.pagingData.collect {}
        }

        viewModel.onQueryChanged("   ")
        viewModel.search()
        runCurrent()

        assertEquals(SearchValidationError.EmptyQuery, viewModel.uiState.value.validationError)
        assertEquals(emptyList<String>(), repository.pagedQueries)
    }

    @Test
    fun `typing three characters searches after debounce`() = runTest {
        val repository = FakeDeveloperRepository()
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.pagingData.collect {}
        }

        viewModel.onQueryChanged("oct")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS - 1)
        runCurrent()
        assertEquals(emptyList<String>(), repository.pagedQueries)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("oct"), repository.pagedQueries)
    }

    @Test
    fun `rapid typing creates pager only for latest query`() = runTest {
        val repository = FakeDeveloperRepository()
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.pagingData.collect {}
        }

        viewModel.onQueryChanged("oct")
        advanceTimeBy(300)
        viewModel.onQueryChanged("octocat")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS)
        runCurrent()

        assertEquals(listOf("octocat"), repository.pagedQueries)
    }

    @Test
    fun `short query waits for manual search`() = runTest {
        val repository = FakeDeveloperRepository()
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.pagingData.collect {}
        }

        viewModel.onQueryChanged("ab")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS)
        runCurrent()
        assertEquals(emptyList<String>(), repository.pagedQueries)

        viewModel.search()
        runCurrent()
        assertEquals(listOf("ab"), repository.pagedQueries)
    }

    @Test
    fun `manual search cancels pending automatic search`() = runTest {
        val repository = FakeDeveloperRepository()
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.pagingData.collect {}
        }

        viewModel.onQueryChanged("octocat")
        viewModel.search()
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS)
        runCurrent()

        assertEquals(listOf("octocat"), repository.pagedQueries)
    }

    @Test
    fun `editing after search clears submitted state and old paging data`() = runTest {
        val repository = FakeDeveloperRepository()
        val viewModel = SearchViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.pagingData.collect {}
        }
        viewModel.onQueryChanged("octocat")
        viewModel.search()
        runCurrent()

        viewModel.onQueryChanged("new")
        runCurrent()

        assertFalse(viewModel.uiState.value.hasSubmittedSearch)
        assertNull(viewModel.uiState.value.validationError)
    }
}

private class FakeDeveloperRepository(
    private val lastSearch: LastSearch? = null,
) : DeveloperRepository {
    val dataStatus = MutableStateFlow<SearchDataStatus>(SearchDataStatus.Unknown)
    override val searchDataStatus: StateFlow<SearchDataStatus> = dataStatus

    val pagedQueries = mutableListOf<String>()

    override suspend fun getLastSearch(): LastSearch? = lastSearch

    override fun getPagedDevelopers(query: String): Flow<PagingData<DeveloperSummary>> {
        pagedQueries += query
        return flowOf(PagingData.empty())
    }

    override suspend fun searchDevelopers(
        query: String,
        page: Int,
        pageSize: Int,
    ): DeveloperRepositoryResult<DeveloperSearchPage> = error("Not needed by ViewModel tests")

    override suspend fun getDeveloperProfile(
        username: String,
    ): DeveloperRepositoryResult<DeveloperProfile> = error("Not needed by ViewModel tests")

    override suspend fun getDeveloperRepositories(
        username: String,
        page: Int,
        pageSize: Int,
    ): DeveloperRepositoryResult<List<DeveloperRepositorySummary>> =
        error("Not needed by ViewModel tests")

    override suspend fun getDeveloperRecentActivity(
        username: String,
        pageSize: Int,
    ): DeveloperRepositoryResult<List<DeveloperActivity>> =
        error("Not needed by ViewModel tests")
}
