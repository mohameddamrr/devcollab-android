package com.mohamedamr.devcollab.domain.model

data class SavedDeveloper(
    val githubId: Long,
    val login: String,
    val avatarUrl: String,
    val profileUrl: String,
    val savedAtEpochMillis: Long,
)
