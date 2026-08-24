package com.mohamedamr.devcollab.data.github.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubContributorDto(
    val login: String? = null,
    val id: Long? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val type: String? = null,
    @SerialName("site_admin") val isSiteAdmin: Boolean = false,
    val contributions: Int = 0,
)
