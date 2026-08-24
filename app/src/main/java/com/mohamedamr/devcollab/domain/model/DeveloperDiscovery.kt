package com.mohamedamr.devcollab.domain.model

sealed interface DiscoveryRequest {
    data class Technologies(val technologies: List<String>) : DiscoveryRequest
    data class Repository(val owner: String, val name: String) : DiscoveryRequest
}

data class DiscoveryCandidate(
    val developer: DeveloperSummary,
    val score: Int,
    val evidence: List<DiscoveryEvidence>,
)

data class DiscoveryEvidence(
    val repositoryId: Long,
    val repositoryFullName: String,
    val repositoryUrl: String,
    val contributions: Int,
    val matchedTechnologies: List<String>,
)

data class DiscoveryResult(
    val candidates: List<DiscoveryCandidate>,
    val repositoriesInspected: Int,
)
