package com.mohamedamr.devcollab.data.github.mapper

import com.mohamedamr.devcollab.data.github.remote.dto.GitHubSearchResponseDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserSummaryDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserDetailDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubRepositoryDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubEventDto
import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.model.DeveloperRepositorySummary
import com.mohamedamr.devcollab.domain.model.DeveloperActivity
import com.mohamedamr.devcollab.domain.model.DeveloperActivityKind
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

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

fun GitHubRepositoryDto.toDomain() = DeveloperRepositorySummary(
    githubId = id,
    name = name,
    fullName = fullName,
    repositoryUrl = htmlUrl,
    description = description.nonBlankOrNull(),
    primaryLanguage = language.nonBlankOrNull(),
    starCount = starCount,
    forkCount = forkCount,
    openIssueCount = openIssueCount,
    isFork = fork,
    isArchived = archived,
    isDisabled = disabled,
    updatedAt = updatedAt,
    pushedAt = pushedAt,
)

fun GitHubEventDto.toDomain() = DeveloperActivity(
    eventId = id,
    kind = when (type) {
        "PushEvent" -> DeveloperActivityKind.Push
        "PullRequestEvent" -> DeveloperActivityKind.PullRequest
        "IssuesEvent" -> DeveloperActivityKind.Issue
        "CreateEvent" -> DeveloperActivityKind.Create
        "ForkEvent" -> DeveloperActivityKind.Fork
        "WatchEvent" -> DeveloperActivityKind.Watch
        "ReleaseEvent" -> DeveloperActivityKind.Release
        else -> DeveloperActivityKind.Other
    },
    rawEventType = type,
    repositoryId = repo.id,
    repositoryName = repo.name,
    action = payload["action"]?.jsonPrimitive?.contentOrNull,
    commitCount = if (type == "PushEvent") {
        payload["size"]?.jsonPrimitive?.intOrNull
    } else {
        null
    },
    createdAt = createdAt,
)

private fun String.toDeveloperAccountType() = when (lowercase()) {
    "user" -> DeveloperAccountType.User
    "organization" -> DeveloperAccountType.Organization
    "bot" -> DeveloperAccountType.Bot
    else -> DeveloperAccountType.Unknown
}

private fun String?.nonBlankOrNull() = this?.trim()?.takeIf(String::isNotEmpty)
