package com.mohamedamr.devcollab.data.github.remote

import com.mohamedamr.devcollab.data.github.remote.dto.GitHubSearchResponseDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubUserDetailDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApiService {
    @GET("search/users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("page") page: Int = DEFAULT_PAGE,
        @Query("per_page") perPage: Int = DEFAULT_PAGE_SIZE,
    ): Response<GitHubSearchResponseDto>

    @GET("users/{username}")
    suspend fun getUser(
        @Path("username") username: String,
    ): Response<GitHubUserDetailDto>

    companion object {
        const val DEFAULT_PAGE = 1
        const val DEFAULT_PAGE_SIZE = 30
        const val MAX_PAGE_SIZE = 100
    }
}
