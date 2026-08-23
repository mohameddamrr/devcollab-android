package com.mohamedamr.devcollab.data.github.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubUserSummaryDto(
    val login: String,
    val id: Long,
    @SerialName("avatar_url")
    val avatarUrl: String,
    @SerialName("html_url")
    val htmlUrl: String,
    val type: String,
    @SerialName("site_admin")
    val isSiteAdmin: Boolean,
    val score: Double,
)
