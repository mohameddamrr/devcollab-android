package com.mohamedamr.devcollab.domain.model

data class DeveloperActivity(
    val eventId: String,
    val kind: DeveloperActivityKind,
    val rawEventType: String,
    val repositoryId: Long,
    val repositoryName: String,
    val action: String?,
    val commitCount: Int?,
    val createdAt: String,
)

enum class DeveloperActivityKind {
    Push,
    PullRequest,
    Issue,
    Create,
    Fork,
    Watch,
    Release,
    Other,
}

data class RecentlyActiveRepository(
    val repositoryName: String,
    val activityCount: Int,
)

fun mostActiveRecently(
    activities: List<DeveloperActivity>,
): List<RecentlyActiveRepository> = activities
    .groupingBy(DeveloperActivity::repositoryName)
    .eachCount()
    .map { (repositoryName, count) ->
        RecentlyActiveRepository(repositoryName, count)
    }
    .sortedWith(
        compareByDescending<RecentlyActiveRepository> { it.activityCount }
            .thenBy { it.repositoryName.lowercase() },
    )
