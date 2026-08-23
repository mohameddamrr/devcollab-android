package com.mohamedamr.devcollab.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_remote_keys")
data class SearchRemoteKeyEntity(
    @PrimaryKey
    val query: String,
    val nextPage: Int?,
    val endReached: Boolean,
    val totalCount: Int,
    val cacheUpdatedAtEpochMillis: Long,
)
