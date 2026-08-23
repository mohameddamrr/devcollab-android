package com.mohamedamr.devcollab.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DeveloperActivityTest {
    @Test
    fun `most active recently aggregates repositories and uses deterministic ordering`() {
        val activities = listOf(
            activity("1", "square/retrofit"),
            activity("2", "square/okhttp"),
            activity("3", "square/retrofit"),
            activity("4", "alpha/project"),
        )

        assertEquals(
            listOf(
                RecentlyActiveRepository("square/retrofit", 2),
                RecentlyActiveRepository("alpha/project", 1),
                RecentlyActiveRepository("square/okhttp", 1),
            ),
            mostActiveRecently(activities),
        )
    }

    @Test
    fun `most active recently handles empty input`() {
        assertEquals(
            emptyList<RecentlyActiveRepository>(),
            mostActiveRecently(emptyList()),
        )
    }

    private fun activity(id: String, repositoryName: String) = DeveloperActivity(
        eventId = id,
        kind = DeveloperActivityKind.Push,
        rawEventType = "PushEvent",
        repositoryId = 1L,
        repositoryName = repositoryName,
        action = null,
        commitCount = 1,
        createdAt = "2026-08-24T10:00:00Z",
    )
}
