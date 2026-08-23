package com.mohamedamr.devcollab.data.github.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

internal object GitHubClientFactory {
    private const val BASE_URL = "https://api.github.com/"
    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 20L
    private const val WRITE_TIMEOUT_SECONDS = 20L
    private const val CALL_TIMEOUT_SECONDS = 30L

    fun create(
        isDebugBuild: Boolean,
        accessTokenProvider: () -> String? = { null },
        baseUrl: String = BASE_URL,
    ): GitHubApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            redactHeader(GitHubRequestInterceptor.AUTHORIZATION_HEADER)
            level = if (isDebugBuild) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(GitHubRequestInterceptor(accessTokenProvider))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(
                githubJson.asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(GitHubApiService::class.java)
    }
}
