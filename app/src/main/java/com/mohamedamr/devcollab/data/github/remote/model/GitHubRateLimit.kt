package com.mohamedamr.devcollab.data.github.remote.model

data class GitHubRateLimit(
    val limit: Int?,
    val remaining: Int?,
    val used: Int?,
    val resetAtEpochSeconds: Long?,
    val resource: String?,
    val retryAfterSeconds: Long?,
)
