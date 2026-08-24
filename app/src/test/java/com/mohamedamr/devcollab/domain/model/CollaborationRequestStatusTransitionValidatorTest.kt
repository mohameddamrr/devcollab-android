package com.mohamedamr.devcollab.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollaborationRequestStatusTransitionValidatorTest {
    @Test fun `receiver can accept or decline pending request`() {
        assertTrue(CollaborationRequestStatusTransitionValidator.canTransition(CollaborationRequestStatus.PENDING, CollaborationRequestStatus.ACCEPTED, CollaborationRequestActor.RECEIVER))
        assertTrue(CollaborationRequestStatusTransitionValidator.canTransition(CollaborationRequestStatus.PENDING, CollaborationRequestStatus.DECLINED, CollaborationRequestActor.RECEIVER))
    }

    @Test fun `sender can only cancel pending request`() {
        assertTrue(CollaborationRequestStatusTransitionValidator.canTransition(CollaborationRequestStatus.PENDING, CollaborationRequestStatus.CANCELLED, CollaborationRequestActor.SENDER))
        assertFalse(CollaborationRequestStatusTransitionValidator.canTransition(CollaborationRequestStatus.PENDING, CollaborationRequestStatus.ACCEPTED, CollaborationRequestActor.SENDER))
        assertFalse(CollaborationRequestStatusTransitionValidator.canTransition(CollaborationRequestStatus.PENDING, CollaborationRequestStatus.DECLINED, CollaborationRequestActor.SENDER))
    }

    @Test fun `receiver cannot cancel and pending cannot remain pending`() {
        assertFalse(CollaborationRequestStatusTransitionValidator.canTransition(CollaborationRequestStatus.PENDING, CollaborationRequestStatus.CANCELLED, CollaborationRequestActor.RECEIVER))
        assertFalse(CollaborationRequestStatusTransitionValidator.canTransition(CollaborationRequestStatus.PENDING, CollaborationRequestStatus.PENDING, CollaborationRequestActor.RECEIVER))
    }

    @Test fun `terminal states cannot transition`() {
        CollaborationRequestStatus.entries.filter { it != CollaborationRequestStatus.PENDING }.forEach { terminal ->
            CollaborationRequestStatus.entries.forEach { target ->
                assertFalse(CollaborationRequestStatusTransitionValidator.canTransition(terminal, target, CollaborationRequestActor.SENDER))
                assertFalse(CollaborationRequestStatusTransitionValidator.canTransition(terminal, target, CollaborationRequestActor.RECEIVER))
            }
        }
    }
}
