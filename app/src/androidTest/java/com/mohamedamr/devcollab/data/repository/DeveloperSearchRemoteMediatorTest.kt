package com.mohamedamr.devcollab.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.mohamedamr.devcollab.data.local.database.DevCollabDatabase
import com.mohamedamr.devcollab.data.local.entity.CachedDeveloperEntity
import com.mohamedamr.devcollab.data.local.entity.SearchRemoteKeyEntity
import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.model.SearchDataStatus
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPagingApi::class)
class DeveloperSearchRemoteMediatorTest {
    private lateinit var database: DevCollabDatabase

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, DevCollabDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun refreshReplacesCacheAndStoresNextPage() = runTest {
        val mediator = mediator {
            DeveloperRepositoryResult.Success(
                page(developers = List(30) { developer(it.toLong()) }, totalCount = 50),
            )
        }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertEquals(30, database.developerSearchDao().cachedDeveloperCount(QUERY))
        assertEquals(2, database.developerSearchDao().getRemoteKey(QUERY)?.nextPage)
        assertEquals(QUERY, database.developerSearchDao().getLastSearch()?.query)
    }

    @Test
    fun appendKeepsFirstPageAndAddsSecondPage() = runTest {
        var requestedPage = 0
        val mediator = mediator { page ->
            requestedPage = page
            val developers = if (page == 1) {
                List(30) { developer(it.toLong()) }
            } else {
                List(10) { developer(30L + it) }
            }
            DeveloperRepositoryResult.Success(page(developers, totalCount = 40))
        }
        mediator.load(LoadType.REFRESH, emptyState())

        val appendResult = mediator.load(LoadType.APPEND, emptyState())

        assertEquals(2, requestedPage)
        assertTrue(appendResult is RemoteMediator.MediatorResult.Success)
        assertEquals(40, database.developerSearchDao().cachedDeveloperCount(QUERY))
        assertNull(database.developerSearchDao().getRemoteKey(QUERY)?.nextPage)
    }

    @Test
    fun refreshFailureUsesExistingCache() = runTest {
        val successMediator = mediator {
            DeveloperRepositoryResult.Success(page(listOf(developer(1)), totalCount = 1))
        }
        successMediator.load(LoadType.REFRESH, emptyState())
        var reportedStatus: SearchDataStatus = SearchDataStatus.Unknown
        val offlineMediator = mediator(onDataStatusChanged = { reportedStatus = it }) {
            DeveloperRepositoryResult.Failure(DeveloperRepositoryError.NetworkUnavailable)
        }

        val result = offlineMediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertEquals(1, database.developerSearchDao().cachedDeveloperCount(QUERY))
        assertEquals(SearchDataStatus.Cached(123L), reportedStatus)
    }

    @Test
    fun refreshFailureWithoutCacheReturnsError() = runTest {
        val mediator = mediator {
            DeveloperRepositoryResult.Failure(DeveloperRepositoryError.NetworkUnavailable)
        }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
    }

    @Test
    fun prependEndsWithoutCallingGitHub() = runTest {
        var remoteCalls = 0
        val mediator = mediator {
            remoteCalls++
            DeveloperRepositoryResult.Success(page(emptyList(), totalCount = 0))
        }

        val result = mediator.load(LoadType.PREPEND, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(0, remoteCalls)
    }

    @Test
    fun appendFailureReturnsErrorAndKeepsFirstPageCache() = runTest {
        val mediator = mediator { requestedPage ->
            if (requestedPage == 1) {
                DeveloperRepositoryResult.Success(
                    page(List(30) { developer(it.toLong()) }, totalCount = 60),
                )
            } else {
                DeveloperRepositoryResult.Failure(DeveloperRepositoryError.NetworkUnavailable)
            }
        }
        mediator.load(LoadType.REFRESH, emptyState())

        val result = mediator.load(LoadType.APPEND, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
        assertEquals(30, database.developerSearchDao().cachedDeveloperCount(QUERY))
        assertEquals(2, database.developerSearchDao().getRemoteKey(QUERY)?.nextPage)
    }

    @Test
    fun emptyRefreshClearsStaleRowsAndStoresRestorableSearch() = runTest {
        val populatedMediator = mediator {
            DeveloperRepositoryResult.Success(page(listOf(developer(1)), totalCount = 1))
        }
        populatedMediator.load(LoadType.REFRESH, emptyState())
        val emptyMediator = mediator {
            DeveloperRepositoryResult.Success(page(emptyList(), totalCount = 0))
        }

        val result = emptyMediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(0, database.developerSearchDao().cachedDeveloperCount(QUERY))
        assertEquals(QUERY, database.developerSearchDao().getLastSearch()?.query)
        assertEquals(0, database.developerSearchDao().getLastSearch()?.totalCount)
    }

    @Test
    fun githubSearchCapStopsPaginationAtOneThousandResults() = runTest {
        database.developerSearchDao().upsertRemoteKey(
            SearchRemoteKeyEntity(
                query = QUERY,
                nextPage = 34,
                endReached = false,
                totalCount = 5_000,
                cacheUpdatedAtEpochMillis = 100L,
            ),
        )
        val mediator = mediator { requestedPage ->
            assertEquals(34, requestedPage)
            DeveloperRepositoryResult.Success(
                page(
                    developers = List(10) { developer(990L + it) },
                    totalCount = 5_000,
                ),
            )
        }

        val result = mediator.load(LoadType.APPEND, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertNull(database.developerSearchDao().getRemoteKey(QUERY)?.nextPage)
    }

    private fun mediator(
        onDataStatusChanged: (SearchDataStatus) -> Unit = {},
        loader: suspend (page: Int) -> DeveloperRepositoryResult<DeveloperSearchPage>,
    ) = DeveloperSearchRemoteMediator(
        query = QUERY,
        dao = database.developerSearchDao(),
        loadRemotePage = { page, _ -> loader(page) },
        currentTimeMillis = { 123L },
        onDataStatusChanged = onDataStatusChanged,
    )

    private fun emptyState() = PagingState<Int, CachedDeveloperEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = 30),
        leadingPlaceholderCount = 0,
    )

    private fun page(
        developers: List<DeveloperSummary>,
        totalCount: Int,
    ) = DeveloperSearchPage(developers, totalCount, isIncomplete = false)

    private fun developer(id: Long) = DeveloperSummary(
        githubId = id,
        login = "developer$id",
        avatarUrl = "avatar",
        profileUrl = "profile",
        accountType = DeveloperAccountType.User,
        isSiteAdmin = false,
    )

    private companion object {
        const val QUERY = "kotlin"
    }
}
