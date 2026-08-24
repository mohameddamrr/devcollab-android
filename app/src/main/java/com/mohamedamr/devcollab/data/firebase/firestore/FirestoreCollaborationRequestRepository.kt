package com.mohamedamr.devcollab.data.firebase.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mohamedamr.devcollab.domain.model.CollaborationRequest
import com.mohamedamr.devcollab.domain.model.CollaborationRequestActor
import com.mohamedamr.devcollab.domain.model.CollaborationRequestDraft
import com.mohamedamr.devcollab.domain.model.CollaborationRequestStatus
import com.mohamedamr.devcollab.domain.model.CollaborationRequestStatusTransitionValidator
import com.mohamedamr.devcollab.domain.repository.CollaborationRequestRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreCollaborationRequestRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : CollaborationRequestRepository {
    override suspend fun create(draft: CollaborationRequestDraft): String {
        val senderUid = authenticatedUid()
        require(senderUid != draft.receiverUid) { "A collaboration request cannot be sent to yourself" }
        val document = firestore.collection(COLLECTION).document()
        document.set(
            mapOf(
                "senderUid" to senderUid,
                "receiverUid" to draft.receiverUid,
                "senderGithubUserId" to draft.senderGithubUserId,
                "receiverGithubUserId" to draft.receiverGithubUserId,
                "projectName" to draft.projectName.trim(),
                "projectDescription" to draft.projectDescription.trim(),
                "technologies" to draft.technologies.map(String::trim).filter(String::isNotEmpty).distinct(),
                "collaborationType" to draft.collaborationType.trim(),
                "neededRole" to draft.neededRole.trim(),
                "expectedCommitment" to draft.expectedCommitment.trim(),
                "message" to draft.message.trim(),
                "evidenceReasons" to draft.evidenceReasons.map(String::trim).filter(String::isNotEmpty).distinct(),
                "status" to CollaborationRequestStatus.PENDING.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        return document.id
    }

    override fun observeReceived(): Flow<List<CollaborationRequest>> =
        observeFor("receiverUid", authenticatedUid())

    override fun observeSent(): Flow<List<CollaborationRequest>> =
        observeFor("senderUid", authenticatedUid())

    override suspend fun accept(requestId: String) =
        transition(requestId, CollaborationRequestStatus.ACCEPTED, CollaborationRequestActor.RECEIVER)

    override suspend fun decline(requestId: String) =
        transition(requestId, CollaborationRequestStatus.DECLINED, CollaborationRequestActor.RECEIVER)

    override suspend fun cancel(requestId: String) =
        transition(requestId, CollaborationRequestStatus.CANCELLED, CollaborationRequestActor.SENDER)

    private fun observeFor(field: String, uid: String): Flow<List<CollaborationRequest>> = callbackFlow {
        val registration = firestore.collection(COLLECTION)
            .whereEqualTo(field, uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else {
                    trySend(
                        snapshot?.documents.orEmpty()
                            .mapNotNull(DocumentSnapshot::toRequest)
                            .sortedByDescending { it.createdAtEpochMillis ?: Long.MAX_VALUE },
                    )
                }
            }
        awaitClose { registration.remove() }
    }

    private suspend fun transition(
        requestId: String,
        target: CollaborationRequestStatus,
        actor: CollaborationRequestActor,
    ) {
        val uid = authenticatedUid()
        val document = firestore.collection(COLLECTION).document(requestId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(document)
            check(snapshot.exists()) { "Collaboration request does not exist" }
            val expectedUid = snapshot.getString(if (actor == CollaborationRequestActor.SENDER) "senderUid" else "receiverUid")
            check(expectedUid == uid) { "The authenticated user cannot perform this transition" }
            val current = snapshot.getString("status")?.let(CollaborationRequestStatus::valueOf)
                ?: error("Collaboration request has an invalid status")
            check(CollaborationRequestStatusTransitionValidator.canTransition(current, target, actor)) {
                "Invalid collaboration request transition: $current -> $target"
            }
            transaction.update(document, mapOf("status" to target.name, "updatedAt" to FieldValue.serverTimestamp()))
        }.await()
    }

    private fun authenticatedUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Authentication is required")
}

private fun DocumentSnapshot.toRequest(): CollaborationRequest? {
    val status = getString("status")?.let { runCatching { CollaborationRequestStatus.valueOf(it) }.getOrNull() } ?: return null
    return CollaborationRequest(
        id = id,
        senderUid = getString("senderUid") ?: return null,
        receiverUid = getString("receiverUid") ?: return null,
        senderGithubUserId = getLong("senderGithubUserId") ?: return null,
        receiverGithubUserId = getLong("receiverGithubUserId") ?: return null,
        projectName = getString("projectName").orEmpty(),
        projectDescription = getString("projectDescription").orEmpty(),
        technologies = stringList("technologies"),
        collaborationType = getString("collaborationType").orEmpty(),
        neededRole = getString("neededRole").orEmpty(),
        expectedCommitment = getString("expectedCommitment").orEmpty(),
        message = getString("message").orEmpty(),
        evidenceReasons = stringList("evidenceReasons"),
        status = status,
        createdAtEpochMillis = getTimestamp("createdAt")?.toDate()?.time,
        updatedAtEpochMillis = getTimestamp("updatedAt")?.toDate()?.time,
    )
}

private fun DocumentSnapshot.stringList(field: String): List<String> =
    (get(field) as? List<*>)?.filterIsInstance<String>().orEmpty()

private const val COLLECTION = "collaborationRequests"
