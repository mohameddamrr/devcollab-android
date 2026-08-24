package com.mohamedamr.devcollab.data.github.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRepositoryDto(
    val id: Long,
    val name: String,
    @SerialName("full_name")
    val fullName: String,
    val owner: GitHubRepositoryOwnerDto = GitHubRepositoryOwnerDto(login = "", id = 0),
    @SerialName("html_url")
    val htmlUrl: String,
    val description: String? = null,
    val language: String? = null,
    val topics: List<String> = emptyList(),
    @SerialName("stargazers_count")
    val starCount: Int,
    @SerialName("forks_count")
    val forkCount: Int,
    @SerialName("open_issues_count")
    val openIssueCount: Int,
    val fork: Boolean,
    val archived: Boolean,
    val disabled: Boolean,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("pushed_at")
    val pushedAt: String? = null,
)

@Serializable
data class GitHubRepositoryOwnerDto(
    val login: String,
    val id: Long,
)
