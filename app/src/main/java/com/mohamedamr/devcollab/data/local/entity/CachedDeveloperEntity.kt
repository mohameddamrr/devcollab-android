package com.mohamedamr.devcollab.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cached_developers",
    primaryKeys = ["query", "githubId"],
    indices = [
        Index(value = ["query"]),
        Index(value = ["query", "position"]),
    ],
)
data class CachedDeveloperEntity(
    val query: String,
    val githubId: Long,
    val position: Int,
    val login: String,
    val avatarUrl: String,
    val profileUrl: String,
    val accountType: String,
    val isSiteAdmin: Boolean,
)
