package com.mohamedamr.devcollab.feature.developerdetails

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.model.DeveloperRepositorySummary
import com.mohamedamr.devcollab.ui.theme.DevCollabTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DeveloperDetailsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun successDisplaysProfileHidesMissingFieldsAndRunsActions() {
        var openedUrl: String? = null
        var sharedProfile: DeveloperProfile? = null
        composeRule.setContent {
            DevCollabTheme {
                DeveloperDetailsScreen(
                    uiState = DeveloperDetailsUiState(
                        username = "octocat",
                        result = DeveloperDetailsResultUiState.Success(testProfile),
                        repositories = DeveloperRepositoriesUiState.Success(
                            repositories = listOf(testRepository),
                            canLoadMore = false,
                        ),
                    ),
                    onBack = {},
                    onRetry = {},
                    onOpenGitHub = { openedUrl = it },
                    onShareProfile = { sharedProfile = it },
                )
            }
        }

        composeRule.onNodeWithTag(DETAILS_SUCCESS_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("The Octocat").assertIsDisplayed()
        composeRule.onNodeWithText("Company").assertDoesNotExist()
        composeRule.onNodeWithText("Open GitHub").performScrollTo().performClick()
        composeRule.onNodeWithText("Share").performScrollTo().performClick()
        composeRule.onNodeWithText("Primary Repository Languages")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1 repos • 100%").assertIsDisplayed()

        assertEquals(testProfile.profileUrl, openedUrl)
        assertEquals(testProfile, sharedProfile)
    }

    @Test
    fun retryableErrorDisplaysRetryAndRunsCallback() {
        var retried = false
        composeRule.setContent {
            DevCollabTheme {
                DeveloperDetailsScreen(
                    uiState = DeveloperDetailsUiState(
                        username = "octocat",
                        result = DeveloperDetailsResultUiState.Error(
                            DeveloperDetailsErrorReason.NetworkUnavailable,
                        ),
                    ),
                    onBack = {},
                    onRetry = { retried = true },
                    onOpenGitHub = {},
                    onShareProfile = {},
                )
            }
        }

        composeRule.onNodeWithTag(DETAILS_ERROR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertTrue(retried)
    }

    @Test
    fun notFoundDisplaysBackAndRunsCallback() {
        var navigatedBack = false
        composeRule.setContent {
            DevCollabTheme {
                DeveloperDetailsScreen(
                    uiState = DeveloperDetailsUiState(
                        username = "missing",
                        result = DeveloperDetailsResultUiState.Error(
                            DeveloperDetailsErrorReason.NotFound,
                        ),
                    ),
                    onBack = { navigatedBack = true },
                    onRetry = {},
                    onOpenGitHub = {},
                    onShareProfile = {},
                )
            }
        }

        composeRule.onNodeWithTag(DETAILS_ERROR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Back").performClick()

        assertTrue(navigatedBack)
    }

    @Test
    fun backButtonRunsCallback() {
        var navigatedBack = false
        composeRule.setContent {
            DevCollabTheme {
                DeveloperDetailsScreen(
                    uiState = DeveloperDetailsUiState(username = "octocat"),
                    onBack = { navigatedBack = true },
                    onRetry = {},
                    onOpenGitHub = {},
                    onShareProfile = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(navigatedBack)
    }

    @Test
    fun repositoriesTabDisplaysEvidenceAndOpensSelectedRepository() {
        var openedRepositoryUrl: String? = null
        composeRule.setContent {
            DevCollabTheme {
                DeveloperDetailsScreen(
                    uiState = DeveloperDetailsUiState(
                        username = "octocat",
                        result = DeveloperDetailsResultUiState.Success(testProfile),
                        selectedTab = DeveloperProfileTab.Repositories,
                        repositories = DeveloperRepositoriesUiState.Success(
                            repositories = listOf(testRepository),
                            canLoadMore = false,
                        ),
                    ),
                    onBack = {},
                    onRetry = {},
                    onOpenGitHub = {},
                    onOpenRepository = { openedRepositoryUrl = it },
                    onShareProfile = {},
                )
            }
        }

        composeRule.onNodeWithText("Recently updated repositories")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("hello-compose").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Kotlin").assertIsDisplayed()
        composeRule.onNodeWithText("12 stars").assertIsDisplayed()
        composeRule.onNodeWithTag(REPOSITORY_CARD_TAG_PREFIX + testRepository.githubId)
            .performClick()

        assertEquals(testRepository.repositoryUrl, openedRepositoryUrl)
    }

    private companion object {
        val testProfile = DeveloperProfile(
            githubId = 1L,
            login = "octocat",
            avatarUrl = "",
            profileUrl = "https://github.com/octocat",
            accountType = DeveloperAccountType.User,
            isSiteAdmin = false,
            name = "The Octocat",
            bio = "GitHub mascot",
            company = null,
            location = "San Francisco",
            websiteUrl = null,
            publicEmail = null,
            twitterUsername = null,
            isHireable = null,
            publicRepositoryCount = 8,
            publicGistCount = 2,
            followers = 20,
            following = 5,
            createdAt = "2011-01-25T18:44:36Z",
            updatedAt = "2026-01-01T00:00:00Z",
        )

        val testRepository = DeveloperRepositorySummary(
            githubId = 10L,
            name = "hello-compose",
            fullName = "octocat/hello-compose",
            repositoryUrl = "https://github.com/octocat/hello-compose",
            description = "Compose sample",
            primaryLanguage = "Kotlin",
            starCount = 12,
            forkCount = 3,
            openIssueCount = 1,
            isFork = false,
            isArchived = false,
            isDisabled = false,
            updatedAt = "2026-08-20T10:00:00Z",
            pushedAt = "2026-08-19T10:00:00Z",
        )
    }
}
