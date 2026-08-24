package com.mohamedamr.devcollab.domain.repository

import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.model.SavedDeveloper
import kotlinx.coroutines.flow.Flow

interface SavedDeveloperRepository {
    fun observeSaved(): Flow<List<SavedDeveloper>>
    fun observeIsSaved(githubId: Long): Flow<Boolean>
    suspend fun save(profile: DeveloperProfile)
    suspend fun remove(githubId: Long)
}
