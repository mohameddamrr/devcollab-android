package com.mohamedamr.devcollab.feature.myprofile

import com.mohamedamr.devcollab.domain.model.AuthenticatedAppUser
import com.mohamedamr.devcollab.domain.model.AppMemberProfile

data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isSigningIn: Boolean = false,
    val user: AuthenticatedAppUser? = null,
    val memberProfile: AppMemberProfile? = null,
    val isCreatingMemberProfile: Boolean = false,
    val isEditingProfile: Boolean = false,
    val isSavingProfile: Boolean = false,
    val draftBio: String = "",
    val draftAvailable: Boolean = false,
    val draftInterests: Set<String> = emptySet(),
    val draftProjectTypes: Set<String> = emptySet(),
    val draftRemotePreferred: Boolean = true,
    val draftLocation: String = "",
    val draftContactMethod: String = "",
    val profileValidationMessage: String? = null,
    val errorMessage: String? = null,
)
