package com.mohamedamr.devcollab.feature.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohamedamr.devcollab.domain.model.DiscoveryCandidate

@Composable
fun CollaborationDiscoveryScreen(
    uiState: CollaborationDiscoveryUiState,
    onTechnologiesChange: (String) -> Unit,
    onRepositoryChange: (String) -> Unit,
    onTechnologySearch: () -> Unit,
    onRepositorySearch: () -> Unit,
    onDeveloperClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Evidence-based collaborator discovery", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Candidates are ranked from bounded public repository contribution evidence, not claimed skills.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = uiState.technologies,
            onValueChange = onTechnologiesChange,
            label = { Text("Technologies, separated by commas") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onTechnologySearch, enabled = !uiState.isLoading) {
            Text("Find by technologies")
        }
        OutlinedTextField(
            value = uiState.repository,
            onValueChange = onRepositoryChange,
            label = { Text("Specific repository (owner/name)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onRepositorySearch, enabled = !uiState.isLoading) {
            Text("Find repository contributors")
        }
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            uiState.errorMessage != null -> Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
            uiState.hasSearched && uiState.candidates.isEmpty() -> Text("No contributors found in the inspected public repositories.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    if (uiState.repositoriesInspected > 0) {
                        Text("Inspected ${uiState.repositoriesInspected} relevant public repositories")
                    }
                }
                items(uiState.candidates, key = { it.developer.githubId }) { candidate ->
                    DiscoveryCandidateCard(candidate) { onDeveloperClick(candidate.developer.login) }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryCandidateCard(candidate: DiscoveryCandidate, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("@${candidate.developer.login}", style = MaterialTheme.typography.titleMedium)
            Text("Contributed to ${candidate.evidence.size} relevant public projects")
            candidate.evidence.take(3).forEach { evidence ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(evidence.repositoryFullName, modifier = Modifier.weight(1f))
                    Text("${evidence.contributions} contributions")
                }
                if (evidence.matchedTechnologies.isNotEmpty()) {
                    Text(
                        evidence.matchedTechnologies.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
