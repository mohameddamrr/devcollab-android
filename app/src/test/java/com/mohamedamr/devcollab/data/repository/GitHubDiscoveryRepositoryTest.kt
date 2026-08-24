package com.mohamedamr.devcollab.data.repository

import com.mohamedamr.devcollab.data.github.remote.dto.GitHubContributorDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubRepositoryDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubRepositoryOwnerDto
import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubDiscoveryRepositoryTest {
    @Test
    fun `aggregation deduplicates ids merges evidence and sorts deterministically`() {
        val kotlinRepo = repository(1, "owner/kotlin-app", "Kotlin", listOf("android"))
        val javaRepo = repository(2, "owner/java-lib", "Java", emptyList())

        val ranked = rankDiscoveryCandidates(
            repositories = listOf(
                RepositoryContributions(kotlinRepo, listOf(contributor(7, "zed", 12), contributor(9, "amy", 7))),
                RepositoryContributions(javaRepo, listOf(contributor(7, "zed", 3), contributor(8, "bob", 10))),
            ),
            technologies = listOf("Kotlin", "Android"),
        )

        assertEquals(listOf(7L, 9L, 8L), ranked.map { it.developer.githubId })
        assertEquals(2, ranked.first().evidence.size)
        assertEquals(listOf("Android", "Kotlin"), ranked.first().evidence.last().matchedTechnologies)
        assertEquals(45, ranked.first().score)
    }

    @Test
    fun `anonymous contributors are excluded and equal scores use login then id`() {
        val repo = repository(1, "owner/repo", null, emptyList())
        val ranked = rankDiscoveryCandidates(
            listOf(RepositoryContributions(repo, listOf(GitHubContributorDto(contributions = 99), contributor(2, "beta", 5), contributor(1, "Alpha", 5)))),
            emptyList(),
        )
        assertEquals(listOf(1L, 2L), ranked.map { it.developer.githubId })
    }

    private fun contributor(id: Long, login: String, contributions: Int) = GitHubContributorDto(
        login = login, id = id, avatarUrl = "avatar", htmlUrl = "url", type = "User", contributions = contributions,
    )

    private fun repository(id: Long, fullName: String, language: String?, topics: List<String>) = GitHubRepositoryDto(
        id = id, name = fullName.substringAfter('/'), fullName = fullName,
        owner = GitHubRepositoryOwnerDto(fullName.substringBefore('/'), 100), htmlUrl = "url",
        language = language, topics = topics, starCount = 0, forkCount = 0, openIssueCount = 0,
        fork = false, archived = false, disabled = false, updatedAt = "2026-01-01T00:00:00Z",
    )
}
