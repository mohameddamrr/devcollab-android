package com.mohamedamr.devcollab.data.local.dao

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.mohamedamr.devcollab.data.local.database.DevCollabDatabase
import com.mohamedamr.devcollab.data.local.entity.CachedDeveloperEntity
import com.mohamedamr.devcollab.data.local.entity.LastSearchEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class DeveloperSearchDaoTest {
    private lateinit var database: DevCollabDatabase
    private lateinit var dao: DeveloperSearchDao

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, DevCollabDatabase::class.java).build()
        dao = database.developerSearchDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun replaceSearchStoresOrderedResultsAndLastSearch() = runTest {
        dao.replaceSearch(
            query = "kotlin",
            developers = listOf(
                developer(query = "kotlin", id = 2, position = 1),
                developer(query = "kotlin", id = 1, position = 0),
            ),
            lastSearch = lastSearch("kotlin", totalCount = 2),
        )

        assertEquals(listOf(1L, 2L), dao.getCachedDevelopers("kotlin").map { it.githubId })
        assertEquals("kotlin", dao.getLastSearch()?.query)
        assertEquals(2, dao.getLastSearch()?.totalCount)
    }

    @Test
    fun replacingSameQueryRemovesStaleRows() = runTest {
        dao.replaceSearch(
            query = "kotlin",
            developers = listOf(
                developer("kotlin", id = 1, position = 0),
                developer("kotlin", id = 2, position = 1),
            ),
            lastSearch = lastSearch("kotlin", totalCount = 2),
        )

        dao.replaceSearch(
            query = "kotlin",
            developers = listOf(developer("kotlin", id = 3, position = 0)),
            lastSearch = lastSearch("kotlin", totalCount = 1),
        )

        assertEquals(listOf(3L), dao.getCachedDevelopers("kotlin").map { it.githubId })
    }

    @Test
    fun replacingOneQueryDoesNotDeleteAnotherQueryCache() = runTest {
        dao.replaceSearch(
            query = "kotlin",
            developers = listOf(developer("kotlin", id = 1, position = 0)),
            lastSearch = lastSearch("kotlin", totalCount = 1),
        )
        dao.replaceSearch(
            query = "android",
            developers = listOf(developer("android", id = 2, position = 0)),
            lastSearch = lastSearch("android", totalCount = 1),
        )

        assertEquals(listOf(1L), dao.getCachedDevelopers("kotlin").map { it.githubId })
        assertEquals(listOf(2L), dao.getCachedDevelopers("android").map { it.githubId })
        assertEquals("android", dao.getLastSearch()?.query)
    }

    @Test
    fun emptyReplacementRemainsRestorable() = runTest {
        dao.replaceSearch(
            query = "missing",
            developers = emptyList(),
            lastSearch = lastSearch("missing", totalCount = 0),
        )

        assertEquals(emptyList<CachedDeveloperEntity>(), dao.getCachedDevelopers("missing"))
        assertEquals("missing", dao.getLastSearch()?.query)
    }

    @Test
    fun lastSearchIsAbsentBeforeFirstSuccessfulSnapshot() = runTest {
        assertNull(dao.getLastSearch())
    }

    private fun developer(query: String, id: Long, position: Int) = CachedDeveloperEntity(
        query = query,
        githubId = id,
        position = position,
        login = "developer$id",
        avatarUrl = "avatar$id",
        profileUrl = "https://github.com/developer$id",
        accountType = "User",
        isSiteAdmin = false,
    )

    private fun lastSearch(query: String, totalCount: Int) = LastSearchEntity(
        query = query,
        totalCount = totalCount,
        lastSearchedAtEpochMillis = 123L,
    )
}
