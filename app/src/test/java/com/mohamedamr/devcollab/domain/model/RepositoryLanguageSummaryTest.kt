package com.mohamedamr.devcollab.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RepositoryLanguageSummaryTest {
    @Test
    fun `summary counts detected primary languages across loaded repositories`() {
        val repositories = listOf(
            repository(1, "Kotlin"),
            repository(2, "Kotlin"),
            repository(3, "Java"),
            repository(4, null),
        )

        val summary = summarizePrimaryRepositoryLanguages(repositories)

        assertEquals(
            listOf(
                RepositoryLanguageSummary("Kotlin", 2, 50),
                RepositoryLanguageSummary("Java", 1, 25),
            ),
            summary,
        )
    }

    @Test
    fun `summary is deterministic for ties and ignores blank language values`() {
        val repositories = listOf(
            repository(1, "Python"),
            repository(2, " Java "),
            repository(3, "   "),
        )

        val summary = summarizePrimaryRepositoryLanguages(repositories)

        assertEquals(listOf("Java", "Python"), summary.map { it.language })
        assertEquals(listOf(33, 33), summary.map { it.percentageOfLoadedRepositories })
    }

    @Test
    fun `empty repositories produce empty summary`() {
        assertEquals(emptyList<RepositoryLanguageSummary>(), summarizePrimaryRepositoryLanguages(emptyList()))
    }

    private fun repository(id: Long, language: String?) = DeveloperRepositorySummary(
        githubId = id,
        name = "repo-$id",
        fullName = "developer/repo-$id",
        repositoryUrl = "https://github.com/developer/repo-$id",
        description = null,
        primaryLanguage = language,
        starCount = 0,
        forkCount = 0,
        openIssueCount = 0,
        isFork = false,
        isArchived = false,
        isDisabled = false,
        updatedAt = "2026-01-01T00:00:00Z",
        pushedAt = null,
    )
}
