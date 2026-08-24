package com.mohamedamr.devcollab.data.local.dao

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.mohamedamr.devcollab.data.local.database.DevCollabDatabase
import com.mohamedamr.devcollab.data.local.entity.SavedDeveloperEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SavedDeveloperDaoTest {
    private lateinit var database: DevCollabDatabase
    private lateinit var dao: SavedDeveloperDao

    @Before fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            DevCollabDatabase::class.java,
        ).build()
        dao = database.savedDeveloperDao()
    }

    @After fun closeDatabase() = database.close()

    @Test fun saveObserveAndRemoveDeveloper() = runTest {
        dao.save(SavedDeveloperEntity(42, "octocat", "avatar", "profile", 1000))
        assertEquals(listOf(42L), dao.observeAll().first().map { it.githubId })
        assertEquals(true, dao.observeIsSaved(42).first())

        dao.remove(42)
        assertEquals(emptyList<SavedDeveloperEntity>(), dao.observeAll().first())
        assertEquals(false, dao.observeIsSaved(42).first())
    }
}
