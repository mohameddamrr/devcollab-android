package com.mohamedamr.devcollab.app

import com.mohamedamr.devcollab.data.github.remote.GitHubClientFactory
import com.mohamedamr.devcollab.data.github.remote.GitHubRemoteDataSource
import com.mohamedamr.devcollab.data.repository.DefaultDeveloperRepository
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository

class AppContainer(isDebugBuild: Boolean) {
    val developerRepository: DeveloperRepository by lazy {
        val apiService = GitHubClientFactory.create(isDebugBuild = isDebugBuild)
        val remoteDataSource = GitHubRemoteDataSource(apiService = apiService)
        DefaultDeveloperRepository(remoteDataSource = remoteDataSource)
    }
}
