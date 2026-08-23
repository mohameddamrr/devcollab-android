package com.mohamedamr.devcollab.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.repository.DeveloperPagingException
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import kotlinx.coroutines.CancellationException

class DeveloperSearchPagingSource(
    private val loadPage: suspend (
        page: Int,
        pageSize: Int,
    ) -> DeveloperRepositoryResult<DeveloperSearchPage>,
) : PagingSource<Int, DeveloperSummary>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DeveloperSummary> {
        val page = params.key ?: FIRST_PAGE
        // GitHub uses page numbers. Keeping per_page fixed prevents a refresh load size
        // from changing what page 2 means and causing gaps or duplicate developers.
        val pageSize = PAGE_SIZE

        return try {
            when (
                val result = loadPage(page, pageSize)
            ) {
                is DeveloperRepositoryResult.Success -> {
                    val response = result.data
                    val reachableResultCount = response.totalCount.coerceAtMost(GITHUB_SEARCH_LIMIT)
                    val loadedThrough = ((page - 1) * pageSize) + response.developers.size
                    val reachedEnd = response.developers.isEmpty() || loadedThrough >= reachableResultCount

                    LoadResult.Page(
                        data = response.developers,
                        prevKey = if (page == FIRST_PAGE) null else page - 1,
                        nextKey = if (reachedEnd) null else page + 1,
                    )
                }

                is DeveloperRepositoryResult.Failure -> LoadResult.Error(
                    DeveloperPagingException(result.error),
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, DeveloperSummary>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition) ?: return null
        return anchorPage.prevKey?.plus(1) ?: anchorPage.nextKey?.minus(1)
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 30
        const val GITHUB_SEARCH_LIMIT = 1_000
    }
}
