package com.mohamedamr.devcollab.data.firebase.auth

import androidx.activity.ComponentActivity
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthDataSource(
    private val firebaseAuth: FirebaseAuth,
    private val preferences: SharedPreferences,
) {
    private val loginRevision = MutableStateFlow(0)
    private val firebaseUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }
    val currentUser: Flow<FirebaseUser?> = firebaseUser.combine(loginRevision) { user, _ -> user }

    fun githubLogin(firebaseUid: String): String? =
        preferences.getString(loginKey(firebaseUid), null)

    fun signOut() {
        firebaseAuth.signOut()
    }

    suspend fun signInWithGitHub(activity: ComponentActivity) {
        val pendingResult = firebaseAuth.pendingAuthResult
        val authResult = if (pendingResult != null) {
            pendingResult.await()
        } else {
            val provider = OAuthProvider.newBuilder(GITHUB_PROVIDER_ID, firebaseAuth).build()
            firebaseAuth.startActivityForSignInWithProvider(activity, provider).await()
        }
        persistGitHubLogin(authResult)
    }

    private fun persistGitHubLogin(authResult: AuthResult) {
        val user = authResult.user ?: return
        val profile = authResult.additionalUserInfo?.profile.orEmpty()
        val githubLogin = authResult.additionalUserInfo?.username
            ?: (profile["login"] as? String)
        if (githubLogin.isNullOrBlank()) return
        preferences.edit().putString(loginKey(user.uid), githubLogin).apply()
        loginRevision.value += 1
    }

    private fun loginKey(firebaseUid: String) = "github_login_$firebaseUid"
}

private const val GITHUB_PROVIDER_ID = "github.com"
