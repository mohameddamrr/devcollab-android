package com.mohamedamr.devcollab.data.github.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubErrorResponseDto(
    val message: String,
    @SerialName("documentation_url")
    val documentationUrl: String? = null,
    val status: String? = null,
)
