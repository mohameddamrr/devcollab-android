package com.mohamedamr.devcollab.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "last_search")
data class LastSearchEntity(
    @PrimaryKey
    val singletonId: Int = SINGLETON_ID,
    val query: String,
    val totalCount: Int,
    val lastSearchedAtEpochMillis: Long,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
