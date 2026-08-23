package com.mohamedamr.devcollab.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mohamedamr.devcollab.data.local.entity.CachedDeveloperEntity
import com.mohamedamr.devcollab.data.local.entity.LastSearchEntity
import com.mohamedamr.devcollab.data.local.entity.SearchRemoteKeyEntity

@Dao
interface DeveloperSearchDao {
    @Query(
        """
        SELECT * FROM cached_developers
        WHERE query = :query
        ORDER BY position ASC, githubId ASC
        """,
    )
    fun pagingSource(query: String): PagingSource<Int, CachedDeveloperEntity>

    @Query(
        """
        SELECT * FROM cached_developers
        WHERE query = :query
        ORDER BY position ASC, githubId ASC
        """,
    )
    suspend fun getCachedDevelopers(query: String): List<CachedDeveloperEntity>

    @Query("SELECT COUNT(*) FROM cached_developers WHERE query = :query")
    suspend fun cachedDeveloperCount(query: String): Int

    @Query("SELECT * FROM last_search WHERE singletonId = 0 LIMIT 1")
    suspend fun getLastSearch(): LastSearchEntity?

    @Query("SELECT * FROM search_remote_keys WHERE query = :query LIMIT 1")
    suspend fun getRemoteKey(query: String): SearchRemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevelopers(developers: List<CachedDeveloperEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLastSearch(lastSearch: LastSearchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRemoteKey(remoteKey: SearchRemoteKeyEntity)

    @Query("DELETE FROM cached_developers WHERE query = :query")
    suspend fun deleteDevelopersForQuery(query: String)

    @Transaction
    suspend fun replaceSearch(
        query: String,
        developers: List<CachedDeveloperEntity>,
        lastSearch: LastSearchEntity,
    ) {
        deleteDevelopersForQuery(query)
        insertDevelopers(developers)
        upsertLastSearch(lastSearch)
    }

    @Transaction
    suspend fun replaceSearchPage(
        query: String,
        developers: List<CachedDeveloperEntity>,
        lastSearch: LastSearchEntity,
        remoteKey: SearchRemoteKeyEntity,
    ) {
        deleteDevelopersForQuery(query)
        insertDevelopers(developers)
        upsertLastSearch(lastSearch)
        upsertRemoteKey(remoteKey)
    }

    @Transaction
    suspend fun appendSearchPage(
        developers: List<CachedDeveloperEntity>,
        remoteKey: SearchRemoteKeyEntity,
    ) {
        insertDevelopers(developers)
        upsertRemoteKey(remoteKey)
    }
}
