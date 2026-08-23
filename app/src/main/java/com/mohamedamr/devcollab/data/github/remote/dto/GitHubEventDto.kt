package com.mohamedamr.devcollab.data.github.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class GitHubEventDto(
    val id: String,
    val type: String,
    val repo: GitHubEventRepositoryDto,
    val payload: JsonObject = JsonObject(emptyMap()),
    val public: Boolean,
    @SerialName("created_at")
    val createdAt: String,
)

@Serializable
data class GitHubEventRepositoryDto(
    val id: Long,
    val name: String,
)
