package com.mohamedamr.devcollab.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_developers")
data class SavedDeveloperEntity(
    @PrimaryKey val githubId: Long,
    val login: String,
    val avatarUrl: String,
    val profileUrl: String,
    val savedAtEpochMillis: Long,
)
