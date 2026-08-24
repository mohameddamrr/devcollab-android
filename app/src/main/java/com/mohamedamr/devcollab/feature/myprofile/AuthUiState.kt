package com.mohamedamr.devcollab.feature.myprofile

import com.mohamedamr.devcollab.domain.model.AuthenticatedAppUser
import com.mohamedamr.devcollab.domain.model.AppMemberProfile

data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isSigningIn: Boolean = false,
    val user: AuthenticatedAppUser? = null,
    val memberProfile: AppMemberProfile? = null,
    val isCreatingMemberProfile: Boolean = false,
    val errorMessage: String? = null,
)
