package com.mohamedamr.devcollab.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mohamedamr.devcollab.data.local.dao.DeveloperSearchDao
import com.mohamedamr.devcollab.data.local.entity.CachedDeveloperEntity
import com.mohamedamr.devcollab.data.local.entity.LastSearchEntity
import com.mohamedamr.devcollab.data.local.entity.SearchRemoteKeyEntity

@Database(
    entities = [
        CachedDeveloperEntity::class,
        LastSearchEntity::class,
        SearchRemoteKeyEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class DevCollabDatabase : RoomDatabase() {
    abstract fun developerSearchDao(): DeveloperSearchDao
}
