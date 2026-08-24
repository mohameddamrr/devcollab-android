package com.mohamedamr.devcollab.domain.model

data class AuthenticatedAppUser(
    val firebaseUid: String,
    val githubUserId: Long,
    val githubLogin: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
)
