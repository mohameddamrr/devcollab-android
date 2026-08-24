package com.mohamedamr.devcollab.feature.myprofile

import androidx.activity.ComponentActivity
import com.mohamedamr.devcollab.domain.model.AuthenticatedAppUser
import com.mohamedamr.devcollab.domain.model.AppMemberProfile
import com.mohamedamr.devcollab.domain.model.CollaborationProfileInput
import com.mohamedamr.devcollab.domain.model.PublicAppMember
import com.mohamedamr.devcollab.domain.repository.AppMemberRepository
import com.mohamedamr.devcollab.domain.repository.AuthRepository
import com.mohamedamr.devcollab.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    @Test
    fun `restored Firebase session becomes signed-in UI state`() = runTest {
        val repository = FakeAuthRepository(testUser)
        val memberRepository = FakeAppMemberRepository()
        val viewModel = AuthViewModel(repository, memberRepository)

        runCurrent()

        assertEquals(false, viewModel.uiState.value.isCheckingSession)
        assertEquals(testUser, viewModel.uiState.value.user)
        assertEquals(testUser, memberRepository.ensuredUser)
        assertEquals(false, viewModel.uiState.value.isCreatingMemberProfile)
        assertEquals("octocat", viewModel.uiState.value.memberProfile?.githubLogin)
    }

    @Test
    fun `sign out delegates to repository and session becomes signed out`() = runTest {
        val repository = FakeAuthRepository(testUser)
        val viewModel = AuthViewModel(repository, FakeAppMemberRepository())
        runCurrent()

        viewModel.signOut()
        runCurrent()

        assertEquals(1, repository.signOutCount)
        assertEquals(null, viewModel.uiState.value.user)
    }

    @Test
    fun `saving collaboration profile updates immutable UI state`() = runTest {
        val authRepository = FakeAuthRepository(testUser)
        val memberRepository = FakeAppMemberRepository()
        val viewModel = AuthViewModel(authRepository, memberRepository)
        runCurrent()

        viewModel.startEditingProfile()
        viewModel.updateBio("Android side projects")
        viewModel.setAvailable(true)
        viewModel.toggleInterest("Android")
        viewModel.saveProfile()
        runCurrent()

        assertEquals(false, viewModel.uiState.value.isEditingProfile)
        assertEquals(true, viewModel.uiState.value.memberProfile?.onboardingCompleted)
        assertEquals(true, viewModel.uiState.value.memberProfile?.availableForCollaboration)
        assertEquals("Android side projects", viewModel.uiState.value.memberProfile?.collaborationBio)
        assertEquals(listOf("Android"), viewModel.uiState.value.memberProfile?.collaborationInterests)
    }

    private companion object {
        val testUser = AuthenticatedAppUser("firebase-1", 42L, "octocat", "Octocat", null, null)
    }
}

private class FakeAppMemberRepository : AppMemberRepository {
    var ensuredUser: AuthenticatedAppUser? = null

    override suspend fun ensureMember(user: AuthenticatedAppUser): AppMemberProfile {
        ensuredUser = user
        return AppMemberProfile(
            firebaseUid = user.firebaseUid,
            githubUserId = user.githubUserId,
            githubLogin = user.githubLogin,
            displayName = user.displayName,
            photoUrl = user.photoUrl,
            onboardingCompleted = false,
            availableForCollaboration = false,
        )
    }

    override suspend fun updateCollaborationProfile(
        firebaseUid: String,
        input: CollaborationProfileInput,
    ): AppMemberProfile = checkNotNull(ensuredUser).let { user ->
        AppMemberProfile(
            firebaseUid = firebaseUid,
            githubUserId = user.githubUserId,
            githubLogin = user.githubLogin,
            displayName = user.displayName,
            photoUrl = user.photoUrl,
            onboardingCompleted = true,
            availableForCollaboration = input.availableForCollaboration,
            collaborationBio = input.collaborationBio,
            collaborationInterests = input.collaborationInterests,
            preferredProjectTypes = input.preferredProjectTypes,
            remotePreferred = input.remotePreferred,
            location = input.location,
            contactMethod = input.contactMethod,
        )
    }

    override suspend fun findPublicMemberByGitHubId(githubUserId: Long): PublicAppMember? = null
}

private class FakeAuthRepository(initialUser: AuthenticatedAppUser?) : AuthRepository {
    private val user = MutableStateFlow(initialUser)
    override val authenticatedUser: Flow<AuthenticatedAppUser?> = user
    var signOutCount = 0

    override suspend fun signInWithGitHub(activity: ComponentActivity) = Unit
    override fun signOut() {
        signOutCount++
        user.value = null
    }
}
