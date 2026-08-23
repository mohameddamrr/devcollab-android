package com.mohamedamr.devcollab.data.github.mapper

import com.mohamedamr.devcollab.data.github.remote.dto.GitHubSearchResponseDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserSummaryDto
import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.DeveloperSummary

fun GitHubSearchResponseDto.toDomain() = DeveloperSearchPage(
    developers = items.map(GitHubUserSummaryDto::toDomain),
    totalCount = totalCount,
    isIncomplete = incompleteResults,
)

private fun GitHubUserSummaryDto.toDomain() = DeveloperSummary(
    githubId = id,
    login = login,
    avatarUrl = avatarUrl,
    profileUrl = htmlUrl,
    accountType = type.toDeveloperAccountType(),
    isSiteAdmin = isSiteAdmin,
)

private fun String.toDeveloperAccountType() = when (lowercase()) {
    "user" -> DeveloperAccountType.User
    "organization" -> DeveloperAccountType.Organization
    "bot" -> DeveloperAccountType.Bot
    else -> DeveloperAccountType.Unknown
}
