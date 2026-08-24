package com.mohamedamr.devcollab.domain.model

data class AppMemberProfile(
    val firebaseUid: String,
    val githubUserId: Long,
    val githubLogin: String,
    val displayName: String?,
    val photoUrl: String?,
    val onboardingCompleted: Boolean,
    val availableForCollaboration: Boolean,
    val collaborationBio: String = "",
    val collaborationInterests: List<String> = emptyList(),
    val preferredProjectTypes: List<String> = emptyList(),
    val remotePreferred: Boolean = true,
    val location: String = "",
    val contactMethod: String = "",
)

data class CollaborationProfileInput(
    val availableForCollaboration: Boolean,
    val collaborationBio: String,
    val collaborationInterests: List<String>,
    val preferredProjectTypes: List<String>,
    val remotePreferred: Boolean,
    val location: String,
    val contactMethod: String,
)
