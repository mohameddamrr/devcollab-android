package com.mohamedamr.devcollab.domain.model

data class DeveloperProfile(
    val githubId: Long,
    val login: String,
    val avatarUrl: String,
    val profileUrl: String,
    val accountType: DeveloperAccountType,
    val isSiteAdmin: Boolean,
    val name: String?,
    val bio: String?,
    val company: String?,
    val location: String?,
    val websiteUrl: String?,
    val publicEmail: String?,
    val twitterUsername: String?,
    val isHireable: Boolean?,
    val publicRepositoryCount: Int,
    val publicGistCount: Int,
    val followers: Int,
    val following: Int,
    val createdAt: String,
    val updatedAt: String,
)
