package com.mohamedamr.devcollab.data.repository

import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.model.DeveloperSearchPage
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import com.mohamedamr.devcollab.domain.repository.DeveloperPagingException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DeveloperSearchPagingSourceTest {
    @Test
    fun `refresh loads first page and provides next key`() = runTest {
        val repository = FakePagingRepository(
            DeveloperRepositoryResult.Success(page(listOf(developer(1)), totalCount = 60)),
        )
        val source = pagingSource(repository)

        val result = source.load(PagingSource.LoadParams.Refresh(null, 30, false))

        val loadedPage = result as PagingSource.LoadResult.Page
        assertEquals(1, repository.requestedPage)
        assertEquals(30, repository.requestedPageSize)
        assertNull(loadedPage.prevKey)
        assertEquals(2, loadedPage.nextKey)
    }

    @Test
    fun `append uses requested page and stops at total count`() = runTest {
        val repository = FakePagingRepository(
            DeveloperRepositoryResult.Success(page(listOf(developer(31)), totalCount = 31)),
        )
        val source = pagingSource(repository)

        val result = source.load(PagingSource.LoadParams.Append(2, 30, false))

        val loadedPage = result as PagingSource.LoadResult.Page
        assertEquals(2, repository.requestedPage)
        assertEquals(1, loadedPage.prevKey)
        assertNull(loadedPage.nextKey)
    }

    @Test
    fun `empty page ends pagination`() = runTest {
        val repository = FakePagingRepository(
            DeveloperRepositoryResult.Success(page(emptyList(), totalCount = 500)),
        )
        val source = pagingSource(repository)

        val result = source.load(PagingSource.LoadParams.Append(2, 30, false))

        assertNull((result as PagingSource.LoadResult.Page).nextKey)
    }

    @Test
    fun `GitHub one thousand result limit ends pagination`() = runTest {
        val repository = FakePagingRepository(
            DeveloperRepositoryResult.Success(
                page(List(10) { developer(991L + it) }, totalCount = 50_000),
            ),
        )
        val source = pagingSource(repository)

        val result = source.load(PagingSource.LoadParams.Append(34, 30, false))

        assertNull((result as PagingSource.LoadResult.Page).nextKey)
    }

    @Test
    fun `repository failure preserves typed error`() = runTest {
        val expectedError = DeveloperRepositoryError.RateLimited(123L)
        val repository = FakePagingRepository(DeveloperRepositoryResult.Failure(expectedError))
        val source = pagingSource(repository)

        val result = source.load(PagingSource.LoadParams.Refresh(null, 30, false))

        val exception = (result as PagingSource.LoadResult.Error).throwable
        assertSame(expectedError, (exception as DeveloperPagingException).repositoryError)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is rethrown`() = runTest {
        val repository = FakePagingRepository(exception = CancellationException())
        pagingSource(repository).load(
            PagingSource.LoadParams.Refresh(null, 30, false),
        )
    }

    private fun page(developers: List<DeveloperSummary>, totalCount: Int) = DeveloperSearchPage(
        developers = developers,
        totalCount = totalCount,
        isIncomplete = false,
    )

    private fun pagingSource(repository: FakePagingRepository) = DeveloperSearchPagingSource {
            page,
            pageSize,
        ->
        repository.searchDevelopers("kotlin", page, pageSize)
    }

    private fun developer(id: Long) = DeveloperSummary(
        githubId = id,
        login = "developer$id",
        avatarUrl = "avatar",
        profileUrl = "profile",
        accountType = DeveloperAccountType.User,
        isSiteAdmin = false,
    )
}

private class FakePagingRepository(
    private val result: DeveloperRepositoryResult<DeveloperSearchPage>? = null,
    private val exception: Exception? = null,
) : DeveloperRepository {
    var requestedPage: Int? = null
    var requestedPageSize: Int? = null

    override fun getPagedDevelopers(query: String): Flow<PagingData<DeveloperSummary>> =
        flowOf(PagingData.empty())

    override suspend fun searchDevelopers(
        query: String,
        page: Int,
        pageSize: Int,
    ): DeveloperRepositoryResult<DeveloperSearchPage> {
        requestedPage = page
        requestedPageSize = pageSize
        exception?.let { throw it }
        return requireNotNull(result)
    }

    override suspend fun getDeveloperProfile(
        username: String,
    ): DeveloperRepositoryResult<DeveloperProfile> = error("Not needed by paging tests")
}
