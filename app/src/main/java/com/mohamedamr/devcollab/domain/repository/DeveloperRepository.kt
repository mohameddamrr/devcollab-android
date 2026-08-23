package com.mohamedamr.devcollab.domain.repository

import androidx.paging.PagingData
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.model.LastSearch
import com.mohamedamr.devcollab.domain.model.SearchDataStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DeveloperRepository {
    val searchDataStatus: StateFlow<SearchDataStatus>

    suspend fun getLastSearch(): LastSearch?

    fun getPagedDevelopers(query: String): Flow<PagingData<DeveloperSummary>>

    suspend fun searchDevelopers(
        query: String,
        page: Int = 1,
        pageSize: Int = 30,
    ): DeveloperRepositoryResult<DeveloperSearchPage>

    suspend fun getDeveloperProfile(
        username: String,
    ): DeveloperRepositoryResult<DeveloperProfile>
}

sealed interface DeveloperRepositoryResult<out T> {
    data class Success<T>(val data: T) : DeveloperRepositoryResult<T>

    data class Failure(val error: DeveloperRepositoryError) : DeveloperRepositoryResult<Nothing>
}

sealed interface DeveloperRepositoryError {
    data object NetworkUnavailable : DeveloperRepositoryError

    data class RateLimited(val resetAtEpochSeconds: Long?) : DeveloperRepositoryError

    data class Server(val statusCode: Int, val message: String?) : DeveloperRepositoryError

    data object InvalidData : DeveloperRepositoryError

    data object Unexpected : DeveloperRepositoryError
}

class DeveloperPagingException(
    val repositoryError: DeveloperRepositoryError,
) : Exception("Unable to load a page of developers.")
