package com.mohamedamr.devcollab.feature.saved

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.mohamedamr.devcollab.domain.model.SavedDeveloper
import com.mohamedamr.devcollab.domain.repository.SavedDeveloperRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SavedDevelopersViewModel(private val repository: SavedDeveloperRepository) : ViewModel() {
    val saved = repository.observeSaved().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun remove(githubId: Long) { viewModelScope.launch { repository.remove(githubId) } }
}

@Composable
fun SavedScreen(
    repository: SavedDeveloperRepository,
    onDeveloperClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: SavedDevelopersViewModel = viewModel(factory = SavedFactory(repository))
    val saved by vm.saved.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Saved developers", style = MaterialTheme.typography.headlineSmall)
        Text("Saved locally on this device and available offline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (saved.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No saved developers yet. Save one from their profile.")
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(saved, key = SavedDeveloper::githubId) { developer ->
                ElevatedCard(onClick = { onDeveloperClick(developer.login) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AsyncImage(developer.avatarUrl, null, Modifier.size(56.dp).clip(CircleShape))
                        Column(Modifier.weight(1f)) { Text("@${developer.login}", style = MaterialTheme.typography.titleMedium); Text("GitHub ID: ${developer.githubId}") }
                        TextButton(onClick = { vm.remove(developer.githubId) }) { Text("Remove") }
                    }
                }
            }
        }
    }
}

private class SavedFactory(private val repository: SavedDeveloperRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = SavedDevelopersViewModel(repository) as T
}
