package com.mohamedamr.devcollab.domain.model

sealed interface SearchDataStatus {
    data object Unknown : SearchDataStatus
    data object Fresh : SearchDataStatus
    data class Cached(val cachedAtEpochMillis: Long) : SearchDataStatus
}
