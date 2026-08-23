package com.mohamedamr.devcollab.feature.discover

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
                    onQueryChanged = { queryChange = it },
                    onSearch = { searchClicked = true },
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
        composeRule.setContent {
            DevCollabTheme {
                DiscoverScreen(
                    uiState = SearchUiState(
                        query = "octocat",
                        result = SearchResultUiState.Success(
                            developers = listOf(testDeveloper),
                            totalCount = 1,
                        ),
                    ),
                    onQueryChanged = {},
                    onSearch = {},
                )
            }
        }

        composeRule.onNodeWithTag(SEARCH_RESULTS_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("@octocat").assertIsDisplayed()
        composeRule.onNodeWithText("GitHub ID: 1").assertIsDisplayed()
        composeRule.onNodeWithText("User").assertIsDisplayed()
    }

    @Test
    fun networkErrorDisplaysRetryAndInvokesCallback() {
        var retried = false
        composeRule.setContent {
            DevCollabTheme {
                DiscoverScreen(
                    uiState = SearchUiState(
                        query = "android",
                        result = SearchResultUiState.Error(
                            SearchErrorReason.NetworkUnavailable,
                        ),
                    ),
                    onQueryChanged = {},
                    onSearch = { retried = true },
                )
            }
        }

        composeRule.onNodeWithTag(SEARCH_ERROR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertTrue(retried)
    }

    private companion object {
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
