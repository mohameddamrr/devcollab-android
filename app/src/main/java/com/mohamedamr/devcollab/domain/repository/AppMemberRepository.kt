package com.mohamedamr.devcollab.domain.repository

import com.mohamedamr.devcollab.domain.model.AppMemberProfile
import com.mohamedamr.devcollab.domain.model.AuthenticatedAppUser
import com.mohamedamr.devcollab.domain.model.CollaborationProfileInput

interface AppMemberRepository {
    suspend fun ensureMember(user: AuthenticatedAppUser): AppMemberProfile
    suspend fun updateCollaborationProfile(
        firebaseUid: String,
        input: CollaborationProfileInput,
    ): AppMemberProfile
}
