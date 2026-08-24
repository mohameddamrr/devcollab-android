package com.mohamedamr.devcollab.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mohamedamr.devcollab.data.local.dao.DeveloperSearchDao
import com.mohamedamr.devcollab.data.local.entity.CachedDeveloperEntity
import com.mohamedamr.devcollab.data.local.entity.LastSearchEntity
import com.mohamedamr.devcollab.data.local.entity.SearchRemoteKeyEntity
import com.mohamedamr.devcollab.data.local.entity.SavedDeveloperEntity
import com.mohamedamr.devcollab.data.local.dao.SavedDeveloperDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedDeveloperEntity::class,
        LastSearchEntity::class,
        SearchRemoteKeyEntity::class,
        SavedDeveloperEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class DevCollabDatabase : RoomDatabase() {
    abstract fun developerSearchDao(): DeveloperSearchDao
    abstract fun savedDeveloperDao(): SavedDeveloperDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `saved_developers` (`githubId` INTEGER NOT NULL, `login` TEXT NOT NULL, `avatarUrl` TEXT NOT NULL, `profileUrl` TEXT NOT NULL, `savedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`githubId`))")
            }
        }
    }
}
