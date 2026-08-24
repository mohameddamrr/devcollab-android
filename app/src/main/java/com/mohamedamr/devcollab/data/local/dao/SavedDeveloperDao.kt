package com.mohamedamr.devcollab.data.local.dao

import androidx.room.*
import com.mohamedamr.devcollab.data.local.entity.SavedDeveloperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedDeveloperDao {
    @Query("SELECT * FROM saved_developers ORDER BY savedAtEpochMillis DESC")
    fun observeAll(): Flow<List<SavedDeveloperEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_developers WHERE githubId = :githubId)")
    fun observeIsSaved(githubId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: SavedDeveloperEntity)

    @Query("DELETE FROM saved_developers WHERE githubId = :githubId")
    suspend fun remove(githubId: Long)
}
