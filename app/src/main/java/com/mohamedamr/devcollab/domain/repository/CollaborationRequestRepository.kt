package com.mohamedamr.devcollab.domain.repository

import com.mohamedamr.devcollab.domain.model.CollaborationRequest
import com.mohamedamr.devcollab.domain.model.CollaborationRequestDraft
import kotlinx.coroutines.flow.Flow

interface CollaborationRequestRepository {
    suspend fun create(draft: CollaborationRequestDraft): String
    fun observeReceived(): Flow<List<CollaborationRequest>>
    fun observeSent(): Flow<List<CollaborationRequest>>
    suspend fun accept(requestId: String)
    suspend fun decline(requestId: String)
    suspend fun cancel(requestId: String)
}
