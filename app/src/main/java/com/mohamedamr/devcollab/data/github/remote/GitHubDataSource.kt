package com.mohamedamr.devcollab.data.github.remote

import com.mohamedamr.devcollab.data.github.remote.dto.GitHubSearchResponseDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserDetailDto
import com.mohamedamr.devcollab.data.github.remote.model.GitHubApiResult

interface GitHubDataSource {
    suspend fun searchUsers(
        query: String,
        page: Int = GitHubApiService.DEFAULT_PAGE,
        perPage: Int = GitHubApiService.DEFAULT_PAGE_SIZE,
    ): GitHubApiResult<GitHubSearchResponseDto>

    suspend fun getUser(username: String): GitHubApiResult<GitHubUserDetailDto>
}
