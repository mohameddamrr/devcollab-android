package com.mohamedamr.devcollab.data.repository

import com.mohamedamr.devcollab.data.local.dao.SavedDeveloperDao
import com.mohamedamr.devcollab.data.local.entity.SavedDeveloperEntity
import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.model.SavedDeveloper
import com.mohamedamr.devcollab.domain.repository.SavedDeveloperRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSavedDeveloperRepository(private val dao: SavedDeveloperDao) : SavedDeveloperRepository {
    override fun observeSaved(): Flow<List<SavedDeveloper>> = dao.observeAll().map { list -> list.map(SavedDeveloperEntity::toDomain) }
    override fun observeIsSaved(githubId: Long): Flow<Boolean> = dao.observeIsSaved(githubId)
    override suspend fun save(profile: DeveloperProfile) = dao.save(
        SavedDeveloperEntity(profile.githubId, profile.login, profile.avatarUrl, profile.profileUrl, System.currentTimeMillis()),
    )
    override suspend fun remove(githubId: Long) = dao.remove(githubId)
}

private fun SavedDeveloperEntity.toDomain() = SavedDeveloper(githubId, login, avatarUrl, profileUrl, savedAtEpochMillis)
