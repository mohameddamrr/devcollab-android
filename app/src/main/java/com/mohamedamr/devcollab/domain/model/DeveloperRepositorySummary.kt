package com.mohamedamr.devcollab.domain.model

data class DeveloperRepositorySummary(
    val githubId: Long,
    val name: String,
    val fullName: String,
    val repositoryUrl: String,
    val description: String?,
    val primaryLanguage: String?,
    val starCount: Int,
    val forkCount: Int,
    val openIssueCount: Int,
    val isFork: Boolean,
    val isArchived: Boolean,
    val isDisabled: Boolean,
    val updatedAt: String,
    val pushedAt: String?,
)
