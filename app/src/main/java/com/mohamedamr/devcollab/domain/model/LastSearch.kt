package com.mohamedamr.devcollab.domain.model

data class LastSearch(
    val query: String,
    val totalCount: Int,
    val lastSearchedAtEpochMillis: Long,
)
