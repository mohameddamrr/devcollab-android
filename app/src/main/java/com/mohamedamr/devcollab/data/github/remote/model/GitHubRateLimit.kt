package com.mohamedamr.devcollab.data.github.remote.model

data class GitHubRateLimit(
    val limit: Int? = null,
    val remaining: Int? = null,
    val used: Int? = null,
    val resetAtEpochSeconds: Long? = null,
    val resource: String? = null,
    val retryAfterSeconds: Long? = null,
)
