package com.mohamedamr.devcollab.app

import android.content.Context
import androidx.room.Room
import com.mohamedamr.devcollab.data.github.remote.GitHubClientFactory
import com.mohamedamr.devcollab.data.github.remote.GitHubRemoteDataSource
import com.mohamedamr.devcollab.data.local.database.DevCollabDatabase
import com.mohamedamr.devcollab.data.repository.DefaultDeveloperRepository
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository

class AppContainer(
    applicationContext: Context,
    isDebugBuild: Boolean,
) {
    private val database: DevCollabDatabase by lazy {
        Room.databaseBuilder(
            context = applicationContext,
            klass = DevCollabDatabase::class.java,
            name = DATABASE_NAME,
        ).build()
    }

    val developerRepository: DeveloperRepository by lazy {
        val apiService = GitHubClientFactory.create(isDebugBuild = isDebugBuild)
        val remoteDataSource = GitHubRemoteDataSource(apiService = apiService)
        DefaultDeveloperRepository(
            remoteDataSource = remoteDataSource,
            developerSearchDao = database.developerSearchDao(),
        )
    }

    private companion object {
        const val DATABASE_NAME = "devcollab.db"
    }
}
