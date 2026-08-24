package com.mohamedamr.devcollab.data.github.remote

import com.mohamedamr.devcollab.data.github.remote.dto.GitHubSearchResponseDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserDetailDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubRepositoryDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubEventDto
import com.mohamedamr.devcollab.data.github.remote.model.GitHubApiResult
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubContributorDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubRepositorySearchResponseDto

interface GitHubDataSource {
    suspend fun searchUsers(
        query: String,
        page: Int = GitHubApiService.DEFAULT_PAGE,
        perPage: Int = GitHubApiService.DEFAULT_PAGE_SIZE,
    ): GitHubApiResult<GitHubSearchResponseDto>

    suspend fun getUser(username: String): GitHubApiResult<GitHubUserDetailDto>

    suspend fun getUserRepositories(
        username: String,
        page: Int = GitHubApiService.DEFAULT_PAGE,
        perPage: Int = GitHubApiService.DEFAULT_PAGE_SIZE,
    ): GitHubApiResult<List<GitHubRepositoryDto>>

    suspend fun getUserPublicEvents(
        username: String,
        page: Int = GitHubApiService.DEFAULT_PAGE,
        perPage: Int = GitHubApiService.DEFAULT_PAGE_SIZE,
    ): GitHubApiResult<List<GitHubEventDto>>

    suspend fun searchRepositories(query: String, perPage: Int): GitHubApiResult<GitHubRepositorySearchResponseDto>
    suspend fun getRepository(owner: String, repository: String): GitHubApiResult<GitHubRepositoryDto>
    suspend fun getRepositoryContributors(owner: String, repository: String, perPage: Int): GitHubApiResult<List<GitHubContributorDto>>
}
