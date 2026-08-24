package com.mohamedamr.devcollab.data.firebase.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mohamedamr.devcollab.domain.model.AppMemberProfile
import com.mohamedamr.devcollab.domain.model.AuthenticatedAppUser
import com.mohamedamr.devcollab.domain.repository.AppMemberRepository
import kotlinx.coroutines.tasks.await

class FirestoreAppMemberRepository(
    private val firestore: FirebaseFirestore,
) : AppMemberRepository {
    override suspend fun ensureMember(user: AuthenticatedAppUser): AppMemberProfile {
        val document = firestore.collection(APP_USERS_COLLECTION).document(user.firebaseUid)
        return firestore.runTransaction { transaction ->
            val existing = transaction.get(document)
            val commonFields = mapOf(
                FIREBASE_UID_FIELD to user.firebaseUid,
                GITHUB_USER_ID_FIELD to user.githubUserId,
                GITHUB_LOGIN_FIELD to user.githubLogin,
                DISPLAY_NAME_FIELD to user.displayName,
                PHOTO_URL_FIELD to user.photoUrl,
                UPDATED_AT_FIELD to FieldValue.serverTimestamp(),
            )
            if (existing.exists()) {
                transaction.update(document, commonFields)
            } else {
                transaction.set(
                    document,
                    commonFields + mapOf(
                        ONBOARDING_COMPLETED_FIELD to false,
                        AVAILABLE_FIELD to false,
                        CREATED_AT_FIELD to FieldValue.serverTimestamp(),
                    ),
                )
            }
            AppMemberProfile(
                firebaseUid = user.firebaseUid,
                githubUserId = user.githubUserId,
                githubLogin = user.githubLogin,
                displayName = user.displayName,
                photoUrl = user.photoUrl,
                onboardingCompleted = existing.getBoolean(ONBOARDING_COMPLETED_FIELD) ?: false,
                availableForCollaboration = existing.getBoolean(AVAILABLE_FIELD) ?: false,
            )
        }.await()
    }
}

private const val APP_USERS_COLLECTION = "appUsers"
private const val FIREBASE_UID_FIELD = "firebaseUid"
private const val GITHUB_USER_ID_FIELD = "githubUserId"
private const val GITHUB_LOGIN_FIELD = "githubLogin"
private const val DISPLAY_NAME_FIELD = "displayName"
private const val PHOTO_URL_FIELD = "photoUrl"
private const val ONBOARDING_COMPLETED_FIELD = "onboardingCompleted"
private const val AVAILABLE_FIELD = "availableForCollaboration"
private const val CREATED_AT_FIELD = "createdAt"
private const val UPDATED_AT_FIELD = "updatedAt"
