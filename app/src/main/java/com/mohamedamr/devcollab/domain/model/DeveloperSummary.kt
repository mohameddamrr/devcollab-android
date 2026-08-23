package com.mohamedamr.devcollab.domain.model

data class DeveloperSummary(
    val githubId: Long,
    val login: String,
    val avatarUrl: String,
    val profileUrl: String,
    val accountType: DeveloperAccountType,
    val isSiteAdmin: Boolean,
)

enum class DeveloperAccountType {
    User,
    Organization,
    Bot,
    Unknown,
}
