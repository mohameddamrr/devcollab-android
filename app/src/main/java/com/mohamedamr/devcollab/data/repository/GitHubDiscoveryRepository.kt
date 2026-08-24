package com.mohamedamr.devcollab.data.repository

import com.mohamedamr.devcollab.data.github.remote.GitHubDataSource
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubContributorDto
import com.mohamedamr.devcollab.data.github.remote.dto.GitHubRepositoryDto
import com.mohamedamr.devcollab.data.github.remote.model.GitHubApiResult
import com.mohamedamr.devcollab.data.github.remote.model.GitHubRemoteError
import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.model.DiscoveryCandidate
import com.mohamedamr.devcollab.domain.model.DiscoveryEvidence
import com.mohamedamr.devcollab.domain.model.DiscoveryRequest
import com.mohamedamr.devcollab.domain.model.DiscoveryResult
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import com.mohamedamr.devcollab.domain.repository.DiscoveryRepository

class GitHubDiscoveryRepository(
    private val remoteDataSource: GitHubDataSource,
) : DiscoveryRepository {
    override suspend fun discover(request: DiscoveryRequest): DeveloperRepositoryResult<DiscoveryResult> {
        val technologies = when (request) {
            is DiscoveryRequest.Technologies -> request.technologies.normalizedTechnologies()
            is DiscoveryRequest.Repository -> emptyList()
        }
        val repositories = when (request) {
            is DiscoveryRequest.Technologies -> {
                if (technologies.isEmpty()) return DeveloperRepositoryResult.Failure(DeveloperRepositoryError.InvalidData)
                when (val result = remoteDataSource.searchRepositories(technologies.joinToString(" "), MAX_REPOSITORIES)) {
                    is GitHubApiResult.Success -> result.data.items.take(MAX_REPOSITORIES)
                    is GitHubApiResult.Failure -> return DeveloperRepositoryResult.Failure(result.error.toDomainError())
                }
            }
            is DiscoveryRequest.Repository -> {
                if (request.owner.isBlank() || request.name.isBlank()) {
                    return DeveloperRepositoryResult.Failure(DeveloperRepositoryError.InvalidData)
                }
                when (val result = remoteDataSource.getRepository(request.owner, request.name)) {
                    is GitHubApiResult.Success -> listOf(result.data)
                    is GitHubApiResult.Failure -> return DeveloperRepositoryResult.Failure(result.error.toDomainError())
                }
            }
        }

        val contributions = mutableListOf<RepositoryContributions>()
        for (repository in repositories) {
            when (
                val result = remoteDataSource.getRepositoryContributors(
                    repository.owner.login,
                    repository.name,
                    MAX_CONTRIBUTORS_PER_REPOSITORY,
                )
            ) {
                is GitHubApiResult.Success -> contributions += RepositoryContributions(repository, result.data)
                is GitHubApiResult.Failure -> return DeveloperRepositoryResult.Failure(result.error.toDomainError())
            }
        }
        return DeveloperRepositoryResult.Success(
            DiscoveryResult(
                candidates = rankDiscoveryCandidates(contributions, technologies),
                repositoriesInspected = repositories.size,
            ),
        )
    }

    companion object {
        const val MAX_REPOSITORIES = 10
        const val MAX_CONTRIBUTORS_PER_REPOSITORY = 10
    }
}

internal data class RepositoryContributions(
    val repository: GitHubRepositoryDto,
    val contributors: List<GitHubContributorDto>,
)

internal fun rankDiscoveryCandidates(
    repositories: List<RepositoryContributions>,
    technologies: List<String>,
): List<DiscoveryCandidate> {
    data class Accumulator(val developer: DeveloperSummary, val evidence: MutableList<DiscoveryEvidence>)
    val candidates = linkedMapOf<Long, Accumulator>()
    repositories.forEach { item ->
        val matches = item.repository.matchingTechnologies(technologies)
        item.contributors.take(GitHubDiscoveryRepository.MAX_CONTRIBUTORS_PER_REPOSITORY).forEach { contributor ->
            val id = contributor.id ?: return@forEach
            val login = contributor.login?.trim()?.takeIf(String::isNotEmpty) ?: return@forEach
            val accumulator = candidates.getOrPut(id) {
                Accumulator(
                    DeveloperSummary(
                        githubId = id,
                        login = login,
                        avatarUrl = contributor.avatarUrl.orEmpty(),
                        profileUrl = contributor.htmlUrl.orEmpty(),
                        accountType = contributor.type.toAccountType(),
                        isSiteAdmin = contributor.isSiteAdmin,
                    ),
                    mutableListOf(),
                )
            }
            accumulator.evidence += DiscoveryEvidence(
                repositoryId = item.repository.id,
                repositoryFullName = item.repository.fullName,
                repositoryUrl = item.repository.htmlUrl,
                contributions = contributor.contributions.coerceAtLeast(0),
                matchedTechnologies = matches,
            )
        }
    }
    return candidates.values.map { accumulator ->
        val evidence = accumulator.evidence.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.repositoryFullName })
        val score = evidence.sumOf { it.contributions.coerceAtMost(100) } +
            evidence.size * 10 + evidence.flatMap { it.matchedTechnologies }.distinct().size * 5
        DiscoveryCandidate(accumulator.developer, score, evidence)
    }.sortedWith(
        compareByDescending<DiscoveryCandidate> { it.score }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.developer.login }
            .thenBy { it.developer.githubId },
    )
}

private fun List<String>.normalizedTechnologies() = asSequence()
    .map(String::trim).filter(String::isNotEmpty).distinctBy { it.lowercase() }.take(5).toList()

private fun GitHubRepositoryDto.matchingTechnologies(technologies: List<String>): List<String> {
    val searchable = buildList {
        add(name); add(description.orEmpty()); add(language.orEmpty()); addAll(topics)
    }.joinToString(" ").lowercase()
    return technologies.filter { searchable.contains(it.lowercase()) }.sortedWith(String.CASE_INSENSITIVE_ORDER)
}

private fun String?.toAccountType() = when (this?.lowercase()) {
    "user" -> DeveloperAccountType.User
    "organization" -> DeveloperAccountType.Organization
    "bot" -> DeveloperAccountType.Bot
    else -> DeveloperAccountType.Unknown
}

private fun GitHubRemoteError.toDomainError(): DeveloperRepositoryError = when (this) {
    is GitHubRemoteError.Network -> DeveloperRepositoryError.NetworkUnavailable
    is GitHubRemoteError.RateLimited -> DeveloperRepositoryError.RateLimited(rateLimit.resetAtEpochSeconds)
    is GitHubRemoteError.Http -> DeveloperRepositoryError.Server(statusCode, message)
    is GitHubRemoteError.InvalidResponse -> DeveloperRepositoryError.InvalidData
    is GitHubRemoteError.Unexpected -> DeveloperRepositoryError.Unexpected
}
