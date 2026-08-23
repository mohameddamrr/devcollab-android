package com.mohamedamr.devcollab.feature.discover

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
                    pagingData = flowOf(PagingData.empty()),
                    onQueryChanged = { queryChange = it },
                    onSearch = { searchClicked = true },
                    onDeveloperClick = {},
                )
            }
        }

        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextInput("octocat")
        composeRule.onNodeWithContentDescription("Search").performClick()

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
                    pagingData = flowOf(PagingData.from(listOf(testDeveloper))),
                    onQueryChanged = {},
                    onSearch = {},
                    onDeveloperClick = { selectedUsername = it },
                )
            }
        }

        composeRule.onNodeWithTag(SEARCH_RESULTS_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("@octocat").assertIsDisplayed()
        composeRule.onNodeWithText("GitHub ID: 1").assertIsDisplayed()
        composeRule.onNodeWithText("User").assertIsDisplayed()
        composeRule.onNodeWithText("@octocat").performClick()
        assertEquals("octocat", selectedUsername)
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
