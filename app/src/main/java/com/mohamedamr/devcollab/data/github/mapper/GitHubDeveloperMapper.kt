package com.mohamedamr.devcollab.data.github.mapper

import com.mohamedamr.devcollab.data.github.remote.dto.GitHubSearchResponseDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserSummaryDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserDetailDto
import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.model.DeveloperProfile

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

fun GitHubUserDetailDto.toDomain() = DeveloperProfile(
    githubId = id,
    login = login,
    avatarUrl = avatarUrl,
    profileUrl = htmlUrl,
    accountType = type.toDeveloperAccountType(),
    isSiteAdmin = isSiteAdmin,
    name = name.nonBlankOrNull(),
    bio = bio.nonBlankOrNull(),
    company = company.nonBlankOrNull(),
    location = location.nonBlankOrNull(),
    websiteUrl = blog.nonBlankOrNull(),
    publicEmail = email.nonBlankOrNull(),
    twitterUsername = twitterUsername.nonBlankOrNull(),
    isHireable = hireable,
    publicRepositoryCount = publicRepositoryCount,
    publicGistCount = publicGistCount,
    followers = followers,
    following = following,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun String.toDeveloperAccountType() = when (lowercase()) {
    "user" -> DeveloperAccountType.User
    "organization" -> DeveloperAccountType.Organization
    "bot" -> DeveloperAccountType.Bot
    else -> DeveloperAccountType.Unknown
}

private fun String?.nonBlankOrNull() = this?.trim()?.takeIf(String::isNotEmpty)
