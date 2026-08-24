package com.mohamedamr.devcollab.domain.model

data class CollaborationRequest(
    val id: String,
    val senderUid: String,
    val receiverUid: String,
    val senderGithubUserId: Long,
    val receiverGithubUserId: Long,
    val projectName: String,
    val projectDescription: String,
    val technologies: List<String>,
    val collaborationType: String,
    val neededRole: String,
    val expectedCommitment: String,
    val message: String,
    val evidenceReasons: List<String>,
    val status: CollaborationRequestStatus,
    val createdAtEpochMillis: Long?,
    val updatedAtEpochMillis: Long?,
)

data class CollaborationRequestDraft(
    val receiverUid: String,
    val senderGithubUserId: Long,
    val receiverGithubUserId: Long,
    val projectName: String,
    val projectDescription: String,
    val technologies: List<String>,
    val collaborationType: String,
    val neededRole: String,
    val expectedCommitment: String,
    val message: String,
    val evidenceReasons: List<String>,
)

enum class CollaborationRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    CANCELLED,
}

object CollaborationRequestStatusTransitionValidator {
    fun canTransition(
        from: CollaborationRequestStatus,
        to: CollaborationRequestStatus,
        actor: CollaborationRequestActor,
    ): Boolean = from == CollaborationRequestStatus.PENDING && when (actor) {
        CollaborationRequestActor.RECEIVER ->
            to == CollaborationRequestStatus.ACCEPTED || to == CollaborationRequestStatus.DECLINED
        CollaborationRequestActor.SENDER -> to == CollaborationRequestStatus.CANCELLED
    }
}

enum class CollaborationRequestActor { SENDER, RECEIVER }
