package com.mohamedamr.devcollab.domain.repository

import com.mohamedamr.devcollab.domain.model.DiscoveryRequest
import com.mohamedamr.devcollab.domain.model.DiscoveryResult

interface DiscoveryRepository {
    suspend fun discover(request: DiscoveryRequest): DeveloperRepositoryResult<DiscoveryResult>
}
