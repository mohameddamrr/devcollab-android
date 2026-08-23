package com.mohamedamr.devcollab.data.github.remote

import okhttp3.Interceptor
import okhttp3.Response

class GitHubRequestInterceptor(
    private val accessTokenProvider: () -> String? = { null },
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request()
            .newBuilder()
            .header(ACCEPT_HEADER, GITHUB_JSON_MEDIA_TYPE)
            .header(API_VERSION_HEADER, API_VERSION)

        accessTokenProvider()
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { accessToken ->
                requestBuilder.header(
                    AUTHORIZATION_HEADER,
                    "$BEARER_PREFIX $accessToken",
                )
            }

        return chain.proceed(requestBuilder.build())
    }

    companion object {
        const val ACCEPT_HEADER = "Accept"
        const val AUTHORIZATION_HEADER = "Authorization"
        const val API_VERSION_HEADER = "X-GitHub-Api-Version"

        const val GITHUB_JSON_MEDIA_TYPE = "application/vnd.github+json"
        const val API_VERSION = "2026-03-10"
        const val BEARER_PREFIX = "Bearer"
    }
}
