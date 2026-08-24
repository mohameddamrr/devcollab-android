package com.mohamedamr.devcollab.app

import android.content.Context
import androidx.room.Room
import com.mohamedamr.devcollab.data.github.remote.GitHubClientFactory
import com.mohamedamr.devcollab.data.github.remote.GitHubRemoteDataSource
import com.mohamedamr.devcollab.data.local.database.DevCollabDatabase
import com.mohamedamr.devcollab.data.repository.DefaultDeveloperRepository
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.google.firebase.auth.FirebaseAuth
import com.mohamedamr.devcollab.data.firebase.auth.FirebaseAuthDataSource
import com.mohamedamr.devcollab.data.firebase.auth.FirebaseAuthRepository
import com.mohamedamr.devcollab.domain.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.mohamedamr.devcollab.data.firebase.firestore.FirestoreAppMemberRepository
import com.mohamedamr.devcollab.domain.repository.AppMemberRepository

class AppContainer(
    applicationContext: Context,
    isDebugBuild: Boolean,
) {
    private val database: DevCollabDatabase by lazy {
        Room.databaseBuilder(
            context = applicationContext,
            klass = DevCollabDatabase::class.java,
            name = DATABASE_NAME,
        ).build()
    }

    val developerRepository: DeveloperRepository by lazy {
        val apiService = GitHubClientFactory.create(isDebugBuild = isDebugBuild)
        val remoteDataSource = GitHubRemoteDataSource(apiService = apiService)
        DefaultDeveloperRepository(
            remoteDataSource = remoteDataSource,
            developerSearchDao = database.developerSearchDao(),
        )
    }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(
            dataSource = FirebaseAuthDataSource(
                firebaseAuth = FirebaseAuth.getInstance(),
                preferences = applicationContext.getSharedPreferences(
                    AUTH_PREFERENCES_NAME,
                    Context.MODE_PRIVATE,
                ),
            ),
        )
    }

    val appMemberRepository: AppMemberRepository by lazy {
        FirestoreAppMemberRepository(FirebaseFirestore.getInstance())
    }

    private companion object {
        const val DATABASE_NAME = "devcollab.db"
        const val AUTH_PREFERENCES_NAME = "devcollab_auth"
    }
}
