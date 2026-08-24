package com.mohamedamr.devcollab.domain.model

data class AppMemberProfile(
    val firebaseUid: String,
    val githubUserId: Long,
    val githubLogin: String,
    val displayName: String?,
    val photoUrl: String?,
    val onboardingCompleted: Boolean,
    val availableForCollaboration: Boolean,
)
