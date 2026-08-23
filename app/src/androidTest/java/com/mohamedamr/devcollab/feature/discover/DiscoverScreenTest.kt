package com.mohamedamr.devcollab.feature.discover

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.ui.theme.DevCollabTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.flow.flowOf

class DiscoverScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typingAndPressingSearchSendsEventsUpward() {
        var queryChange = ""
        var searchClicked = false
        composeRule.setContent {
            DevCollabTheme {
                DiscoverScreen(
                    uiState = SearchUiState(),
                    pagingData = settledPagingFlow(),
                    onQueryChanged = { queryChange = it },
                    onSearch = { searchClicked = true },
                    onDeveloperClick = {},
                )
            }
        }

        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("octocat")
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performImeAction()

        assertEquals("octocat", queryChange)
        assertTrue(searchClicked)
    }

    @Test
    fun successStateDisplaysDeveloperEvidence() {
        var selectedUsername: String? = null
        composeRule.setContent {
            DevCollabTheme {
                DiscoverScreen(
                    uiState = SearchUiState(
                        query = "octocat",
                        hasSubmittedSearch = true,
                    ),
                    pagingData = settledPagingFlow(listOf(testDeveloper)),
                    onQueryChanged = {},
                    onSearch = {},
                    onDeveloperClick = { selectedUsername = it },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(SEARCH_RESULTS_TAG)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(SEARCH_RESULTS_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("@octocat").assertIsDisplayed()
        composeRule.onNodeWithText("GitHub ID: 1").assertIsDisplayed()
        composeRule.onNodeWithText("User").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Avatar for octocat").assertIsDisplayed()
        composeRule.onNodeWithText("@octocat").performClick()
        assertEquals("octocat", selectedUsername)
    }

    @Test
    fun emptySearchDisplaysHelpfulMessage() {
        composeRule.setContent {
            DevCollabTheme {
                DiscoverScreen(
                    uiState = SearchUiState(
                        query = "missing-developer",
                        hasSubmittedSearch = true,
                    ),
                    pagingData = settledPagingFlow(),
                    onQueryChanged = {},
                    onSearch = {},
                    onDeveloperClick = {},
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(
                "No developers found. Try a different search.",
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("No developers found. Try a different search.")
            .assertIsDisplayed()
    }

    @Test
    fun cachedSearchDisplaysStaleDataBannerAndResults() {
        composeRule.setContent {
            DevCollabTheme {
                DiscoverScreen(
                    uiState = SearchUiState(
                        query = "octocat",
                        hasSubmittedSearch = true,
                        cachedAtEpochMillis = 123L,
                    ),
                    pagingData = settledPagingFlow(listOf(testDeveloper)),
                    onQueryChanged = {},
                    onSearch = {},
                    onDeveloperClick = {},
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(SEARCH_RESULTS_TAG)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Showing cached results", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("@octocat").assertIsDisplayed()
    }

    @Test
    fun networkErrorDisplaysRetry() {
        composeRule.setContent {
            DevCollabTheme {
                DiscoverScreen(
                    uiState = SearchUiState(
                        query = "android",
                        hasSubmittedSearch = true,
                    ),
                    pagingData = failingPagingFlow(),
                    onQueryChanged = {},
                    onSearch = {},
                    onDeveloperClick = {},
                )
            }
        }

        composeRule.onNodeWithTag(SEARCH_ERROR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    private companion object {
        fun settledPagingFlow(
            developers: List<DeveloperSummary> = emptyList(),
        ) = flowOf(
            PagingData.from(
                data = developers,
                sourceLoadStates = LoadStates(
                    refresh = LoadState.NotLoading(endOfPaginationReached = developers.isEmpty()),
                    prepend = LoadState.NotLoading(endOfPaginationReached = true),
                    append = LoadState.NotLoading(endOfPaginationReached = true),
                ),
            ),
        )

        fun failingPagingFlow() = Pager(
            config = PagingConfig(pageSize = 1),
            pagingSourceFactory = {
                object : PagingSource<Int, DeveloperSummary>() {
                    override suspend fun load(
                        params: LoadParams<Int>,
                    ): LoadResult<Int, DeveloperSummary> = LoadResult.Error(
                        IllegalStateException("test failure"),
                    )

                    override fun getRefreshKey(
                        state: PagingState<Int, DeveloperSummary>,
                    ): Int? = null
                }
            },
        ).flow

        val testDeveloper = DeveloperSummary(
            githubId = 1L,
            login = "octocat",
            avatarUrl = "",
            profileUrl = "https://github.com/octocat",
            accountType = DeveloperAccountType.User,
            isSiteAdmin = false,
        )
    }
}
