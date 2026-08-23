package com.mohamedamr.devcollab.data.local.mapper

import com.mohamedamr.devcollab.data.local.entity.CachedDeveloperEntity
import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.model.DeveloperSummary

fun DeveloperSummary.toCacheEntity(query: String, position: Int) = CachedDeveloperEntity(
    query = query,
    githubId = githubId,
    position = position,
    login = login,
    avatarUrl = avatarUrl,
    profileUrl = profileUrl,
    accountType = accountType.name,
    isSiteAdmin = isSiteAdmin,
)

fun CachedDeveloperEntity.toDomain() = DeveloperSummary(
    githubId = githubId,
    login = login,
    avatarUrl = avatarUrl,
    profileUrl = profileUrl,
    accountType = accountType.toDeveloperAccountType(),
    isSiteAdmin = isSiteAdmin,
)

private fun String.toDeveloperAccountType() = DeveloperAccountType.entries
    .firstOrNull { it.name == this }
    ?: DeveloperAccountType.Unknown
