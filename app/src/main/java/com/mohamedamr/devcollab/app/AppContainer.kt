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
import com.mohamedamr.devcollab.data.repository.GitHubDiscoveryRepository
import com.mohamedamr.devcollab.domain.repository.DiscoveryRepository
import com.mohamedamr.devcollab.data.firebase.firestore.FirestoreCollaborationRequestRepository
import com.mohamedamr.devcollab.domain.repository.CollaborationRequestRepository
import com.mohamedamr.devcollab.data.repository.RoomSavedDeveloperRepository
import com.mohamedamr.devcollab.domain.repository.SavedDeveloperRepository

class AppContainer(
    applicationContext: Context,
    isDebugBuild: Boolean,
) {
    private val githubRemoteDataSource: GitHubRemoteDataSource by lazy {
        GitHubRemoteDataSource(GitHubClientFactory.create(isDebugBuild = isDebugBuild))
    }
    private val database: DevCollabDatabase by lazy {
        Room.databaseBuilder(
            context = applicationContext,
            klass = DevCollabDatabase::class.java,
            name = DATABASE_NAME,
        ).addMigrations(DevCollabDatabase.MIGRATION_1_2).build()
    }

    val developerRepository: DeveloperRepository by lazy {
        DefaultDeveloperRepository(
            remoteDataSource = githubRemoteDataSource,
            developerSearchDao = database.developerSearchDao(),
        )
    }

    val discoveryRepository: DiscoveryRepository by lazy {
        GitHubDiscoveryRepository(githubRemoteDataSource)
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

    val collaborationRequestRepository: CollaborationRequestRepository by lazy {
        FirestoreCollaborationRequestRepository(
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
        )
    }

    val savedDeveloperRepository: SavedDeveloperRepository by lazy {
        RoomSavedDeveloperRepository(database.savedDeveloperDao())
    }

    private companion object {
        const val DATABASE_NAME = "devcollab.db"
        const val AUTH_PREFERENCES_NAME = "devcollab_auth"
    }
}
