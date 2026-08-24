package com.mohamedamr.devcollab.data.firebase.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mohamedamr.devcollab.domain.model.AppMemberProfile
import com.mohamedamr.devcollab.domain.model.AuthenticatedAppUser
import com.mohamedamr.devcollab.domain.model.CollaborationProfileInput
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
                transaction.update(
                    document,
                    commonFields + mapOf(
                        ONBOARDING_COMPLETED_FIELD to (existing.getBoolean(ONBOARDING_COMPLETED_FIELD) ?: false),
                        AVAILABLE_FIELD to (existing.getBoolean(AVAILABLE_FIELD) ?: false),
                        COLLABORATION_BIO_FIELD to existing.getString(COLLABORATION_BIO_FIELD).orEmpty(),
                        COLLABORATION_INTERESTS_FIELD to existing.stringList(COLLABORATION_INTERESTS_FIELD),
                        PROJECT_TYPES_FIELD to existing.stringList(PROJECT_TYPES_FIELD),
                        REMOTE_PREFERRED_FIELD to (existing.getBoolean(REMOTE_PREFERRED_FIELD) ?: true),
                        LOCATION_FIELD to existing.getString(LOCATION_FIELD).orEmpty(),
                        CONTACT_METHOD_FIELD to existing.getString(CONTACT_METHOD_FIELD).orEmpty(),
                    ),
                )
            } else {
                transaction.set(
                    document,
                    commonFields + mapOf(
                        ONBOARDING_COMPLETED_FIELD to false,
                        AVAILABLE_FIELD to false,
                        COLLABORATION_BIO_FIELD to "",
                        COLLABORATION_INTERESTS_FIELD to emptyList<String>(),
                        PROJECT_TYPES_FIELD to emptyList<String>(),
                        REMOTE_PREFERRED_FIELD to true,
                        LOCATION_FIELD to "",
                        CONTACT_METHOD_FIELD to "",
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
                collaborationBio = existing.getString(COLLABORATION_BIO_FIELD).orEmpty(),
                collaborationInterests = existing.stringList(COLLABORATION_INTERESTS_FIELD),
                preferredProjectTypes = existing.stringList(PROJECT_TYPES_FIELD),
                remotePreferred = existing.getBoolean(REMOTE_PREFERRED_FIELD) ?: true,
                location = existing.getString(LOCATION_FIELD).orEmpty(),
                contactMethod = existing.getString(CONTACT_METHOD_FIELD).orEmpty(),
            )
        }.await()
    }

    override suspend fun updateCollaborationProfile(
        firebaseUid: String,
        input: CollaborationProfileInput,
    ): AppMemberProfile {
        val document = firestore.collection(APP_USERS_COLLECTION).document(firebaseUid)
        document.update(
            mapOf(
                AVAILABLE_FIELD to input.availableForCollaboration,
                COLLABORATION_BIO_FIELD to input.collaborationBio.trim(),
                COLLABORATION_INTERESTS_FIELD to input.collaborationInterests.distinct(),
                PROJECT_TYPES_FIELD to input.preferredProjectTypes.distinct(),
                REMOTE_PREFERRED_FIELD to input.remotePreferred,
                LOCATION_FIELD to input.location.trim(),
                CONTACT_METHOD_FIELD to input.contactMethod.trim(),
                ONBOARDING_COMPLETED_FIELD to true,
                UPDATED_AT_FIELD to FieldValue.serverTimestamp(),
            ),
        ).await()
        val snapshot = document.get().await()
        return AppMemberProfile(
            firebaseUid = snapshot.getString(FIREBASE_UID_FIELD) ?: firebaseUid,
            githubUserId = snapshot.getLong(GITHUB_USER_ID_FIELD) ?: 0L,
            githubLogin = snapshot.getString(GITHUB_LOGIN_FIELD).orEmpty(),
            displayName = snapshot.getString(DISPLAY_NAME_FIELD),
            photoUrl = snapshot.getString(PHOTO_URL_FIELD),
            onboardingCompleted = snapshot.getBoolean(ONBOARDING_COMPLETED_FIELD) ?: false,
            availableForCollaboration = snapshot.getBoolean(AVAILABLE_FIELD) ?: false,
            collaborationBio = snapshot.getString(COLLABORATION_BIO_FIELD).orEmpty(),
            collaborationInterests = snapshot.stringList(COLLABORATION_INTERESTS_FIELD),
            preferredProjectTypes = snapshot.stringList(PROJECT_TYPES_FIELD),
            remotePreferred = snapshot.getBoolean(REMOTE_PREFERRED_FIELD) ?: true,
            location = snapshot.getString(LOCATION_FIELD).orEmpty(),
            contactMethod = snapshot.getString(CONTACT_METHOD_FIELD).orEmpty(),
        )
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.stringList(field: String): List<String> =
    (get(field) as? List<*>)?.filterIsInstance<String>().orEmpty()

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
private const val COLLABORATION_BIO_FIELD = "collaborationBio"
private const val COLLABORATION_INTERESTS_FIELD = "collaborationInterests"
private const val PROJECT_TYPES_FIELD = "preferredProjectTypes"
private const val REMOTE_PREFERRED_FIELD = "remotePreferred"
private const val LOCATION_FIELD = "location"
private const val CONTACT_METHOD_FIELD = "contactMethod"
