package com.mohamedamr.devcollab.feature.myprofile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mohamedamr.devcollab.domain.model.AuthenticatedAppUser
import com.mohamedamr.devcollab.ui.theme.DevCollabTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MyProfileScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun signedOutStateOffersGitHubSignIn() {
        var clicked = false
        composeRule.setContent {
            DevCollabTheme {
                MyProfileScreen(AuthUiState(isCheckingSession = false), { clicked = true }, {})
            }
        }
        composeRule.onNodeWithText("Sign in with GitHub").performClick()
        assertTrue(clicked)
    }

    @Test
    fun signedInStateDisplaysIdentityAndSignOut() {
        composeRule.setContent {
            DevCollabTheme {
                MyProfileScreen(
                    AuthUiState(
                        isCheckingSession = false,
                        user = AuthenticatedAppUser(
                            "firebase-1", 42L, "octocat", "Octocat", null, null,
                        ),
                    ),
                    {},
                    {},
                )
            }
        }
        composeRule.onNodeWithText("Octocat").assertIsDisplayed()
        composeRule.onNodeWithText("GitHub ID: 42").assertIsDisplayed()
        composeRule.onNodeWithText("Sign out").assertIsDisplayed()
    }
}
