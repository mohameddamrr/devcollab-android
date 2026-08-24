package com.mohamedamr.devcollab.data.firebase.auth

import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseUser
import com.mohamedamr.devcollab.domain.model.AuthenticatedAppUser
import com.mohamedamr.devcollab.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseAuthRepository(
    private val dataSource: FirebaseAuthDataSource,
) : AuthRepository {
    override val authenticatedUser: Flow<AuthenticatedAppUser?> =
        dataSource.currentUser.map { user -> user.toDomain(dataSource) }

    override suspend fun signInWithGitHub(activity: ComponentActivity) {
        dataSource.signInWithGitHub(activity)
    }

    override fun signOut() {
        dataSource.signOut()
    }
}

private fun FirebaseUser?.toDomain(dataSource: FirebaseAuthDataSource): AuthenticatedAppUser? {
    val firebaseUser = this ?: return null
    val githubIdentity = firebaseUser.providerData.firstOrNull {
        it.providerId == GITHUB_PROVIDER_ID
    } ?: return null
    val githubUserId = githubIdentity.uid.toLongOrNull() ?: return null
    val githubLogin = dataSource.githubLogin(firebaseUser.uid) ?: return null

    return AuthenticatedAppUser(
        firebaseUid = firebaseUser.uid,
        githubUserId = githubUserId,
        githubLogin = githubLogin,
        displayName = githubIdentity.displayName ?: firebaseUser.displayName,
        email = firebaseUser.email,
        photoUrl = firebaseUser.photoUrl?.toString(),
    )
}

private const val GITHUB_PROVIDER_ID = "github.com"
