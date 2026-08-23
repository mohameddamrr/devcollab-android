package com.mohamedamr.devcollab.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.mohamedamr.devcollab.data.local.dao.DeveloperSearchDao
import com.mohamedamr.devcollab.data.local.entity.CachedDeveloperEntity
import com.mohamedamr.devcollab.data.local.entity.LastSearchEntity
import com.mohamedamr.devcollab.data.local.entity.SearchRemoteKeyEntity
import com.mohamedamr.devcollab.data.local.mapper.toCacheEntity
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.SearchDataStatus
import com.mohamedamr.devcollab.domain.repository.DeveloperPagingException
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalPagingApi::class)
class DeveloperSearchRemoteMediator(
    private val query: String,
    private val dao: DeveloperSearchDao,
    private val loadRemotePage: suspend (
        page: Int,
        pageSize: Int,
    ) -> DeveloperRepositoryResult<DeveloperSearchPage>,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val onDataStatusChanged: (SearchDataStatus) -> Unit = {},
) : RemoteMediator<Int, CachedDeveloperEntity>() {
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CachedDeveloperEntity>,
    ): MediatorResult {
        if (loadType == LoadType.PREPEND) {
            return MediatorResult.Success(endOfPaginationReached = true)
        }

        val page = when (loadType) {
            LoadType.REFRESH -> FIRST_PAGE
            LoadType.APPEND -> {
                val remoteKey = dao.getRemoteKey(query)
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
                remoteKey.nextPage
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.PREPEND -> error("PREPEND is handled before page selection.")
        }

        return try {
            when (val result = loadRemotePage(page, PAGE_SIZE)) {
                is DeveloperRepositoryResult.Success -> {
                    cacheSuccessfulPage(loadType, page, result.data)
                    MediatorResult.Success(
                        endOfPaginationReached = calculateEndReached(page, result.data),
                    )
                }

                is DeveloperRepositoryResult.Failure -> {
                    val hasCachedResults = dao.cachedDeveloperCount(query) > 0
                    if (loadType == LoadType.REFRESH && hasCachedResults) {
                        reportCachedFallback()
                        MediatorResult.Success(endOfPaginationReached = true)
                    } else {
                        MediatorResult.Error(DeveloperPagingException(result.error))
                    }
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            val hasCachedResults = dao.cachedDeveloperCount(query) > 0
            if (loadType == LoadType.REFRESH && hasCachedResults) {
                reportCachedFallback()
                MediatorResult.Success(endOfPaginationReached = true)
            } else {
                MediatorResult.Error(exception)
            }
        }
    }

    private suspend fun cacheSuccessfulPage(
        loadType: LoadType,
        page: Int,
        response: DeveloperSearchPage,
    ) {
        val cachedAt = currentTimeMillis()
        val developers = response.developers.mapIndexed { index, developer ->
            developer.toCacheEntity(
                query = query,
                position = ((page - 1) * PAGE_SIZE) + index,
            )
        }
        val endReached = calculateEndReached(page, response)
        val remoteKey = SearchRemoteKeyEntity(
            query = query,
            nextPage = if (endReached) null else page + 1,
            endReached = endReached,
            totalCount = response.totalCount,
            cacheUpdatedAtEpochMillis = cachedAt,
        )

        if (loadType == LoadType.REFRESH) {
            dao.replaceSearchPage(
                query = query,
                developers = developers,
                lastSearch = LastSearchEntity(
                    query = query,
                    totalCount = response.totalCount,
                    lastSearchedAtEpochMillis = cachedAt,
                ),
                remoteKey = remoteKey,
            )
        } else {
            dao.appendSearchPage(developers = developers, remoteKey = remoteKey)
        }
        onDataStatusChanged(SearchDataStatus.Fresh)
    }

    private suspend fun reportCachedFallback() {
        val cachedAt = dao.getRemoteKey(query)?.cacheUpdatedAtEpochMillis ?: 0L
        onDataStatusChanged(SearchDataStatus.Cached(cachedAt))
    }

    private fun calculateEndReached(page: Int, response: DeveloperSearchPage): Boolean {
        val reachableCount = response.totalCount.coerceAtMost(GITHUB_SEARCH_LIMIT)
        val loadedThrough = ((page - 1) * PAGE_SIZE) + response.developers.size
        return response.developers.isEmpty() || loadedThrough >= reachableCount
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 30
        const val GITHUB_SEARCH_LIMIT = 1_000
    }
}
