package com.mohamedamr.devcollab.domain.repository

import androidx.activity.ComponentActivity
import com.mohamedamr.devcollab.domain.model.AuthenticatedAppUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authenticatedUser: Flow<AuthenticatedAppUser?>

    suspend fun signInWithGitHub(activity: ComponentActivity)

    fun signOut()
}
