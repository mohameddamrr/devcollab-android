package com.mohamedamr.devcollab.domain.repository

import com.mohamedamr.devcollab.domain.model.AppMemberProfile
import com.mohamedamr.devcollab.domain.model.AuthenticatedAppUser

interface AppMemberRepository {
    suspend fun ensureMember(user: AuthenticatedAppUser): AppMemberProfile
}
