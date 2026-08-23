package com.mohamedamr.devcollab.domain.model

import kotlin.math.roundToInt

data class RepositoryLanguageSummary(
    val language: String,
    val repositoryCount: Int,
    val percentageOfLoadedRepositories: Int,
)

fun summarizePrimaryRepositoryLanguages(
    repositories: List<DeveloperRepositorySummary>,
): List<RepositoryLanguageSummary> {
    if (repositories.isEmpty()) return emptyList()

    return repositories
        .mapNotNull { repository ->
            repository.primaryLanguage?.trim()?.takeIf(String::isNotEmpty)
        }
        .groupingBy { it }
        .eachCount()
        .map { (language, count) ->
            RepositoryLanguageSummary(
                language = language,
                repositoryCount = count,
                percentageOfLoadedRepositories =
                    ((count.toDouble() / repositories.size) * 100).roundToInt(),
            )
        }
        .sortedWith(
            compareByDescending<RepositoryLanguageSummary> { it.repositoryCount }
                .thenBy { it.language.lowercase() },
        )
}
