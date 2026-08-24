package com.mohamedamr.devcollab.feature.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mohamedamr.devcollab.domain.model.DiscoveryCandidate
import com.mohamedamr.devcollab.domain.model.DiscoveryRequest
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError
import com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryResult
import com.mohamedamr.devcollab.domain.repository.DiscoveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CollaborationDiscoveryUiState(
    val technologies: String = "",
    val repository: String = "",
    val isLoading: Boolean = false,
    val candidates: List<DiscoveryCandidate> = emptyList(),
    val repositoriesInspected: Int = 0,
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
)

class CollaborationDiscoveryViewModel(
    private val repository: DiscoveryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollaborationDiscoveryUiState())
    val uiState = _uiState.asStateFlow()

    fun onTechnologiesChange(value: String) = _uiState.update { it.copy(technologies = value) }
    fun onRepositoryChange(value: String) = _uiState.update { it.copy(repository = value) }

    fun discoverByTechnologies() {
        val technologies = _uiState.value.technologies.split(',').map(String::trim).filter(String::isNotEmpty)
        if (technologies.isEmpty()) return showValidation("Enter at least one technology")
        discover(DiscoveryRequest.Technologies(technologies))
    }

    fun discoverByRepository() {
        val parts = _uiState.value.repository.trim().split('/').filter(String::isNotBlank)
        if (parts.size != 2) return showValidation("Use repository format owner/name")
        discover(DiscoveryRequest.Repository(parts[0], parts[1]))
    }

    private fun discover(request: DiscoveryRequest) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null, hasSearched = true) }
        viewModelScope.launch {
            when (val result = repository.discover(request)) {
                is DeveloperRepositoryResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        candidates = result.data.candidates,
                        repositoriesInspected = result.data.repositoriesInspected,
                    )
                }
                is DeveloperRepositoryResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, candidates = emptyList(), errorMessage = result.error.message())
                }
            }
        }
    }

    private fun showValidation(message: String) {
        _uiState.update { it.copy(errorMessage = message, hasSearched = true, candidates = emptyList()) }
    }
}

private fun DeveloperRepositoryError.message() = when (this) {
    DeveloperRepositoryError.NetworkUnavailable -> "Unable to reach GitHub"
    is DeveloperRepositoryError.RateLimited -> "GitHub rate limit reached. Try again later."
    is DeveloperRepositoryError.Server -> "GitHub request failed ($statusCode)"
    DeveloperRepositoryError.InvalidData -> "GitHub returned invalid discovery data"
    DeveloperRepositoryError.Unexpected -> "Unexpected discovery error"
}

class CollaborationDiscoveryViewModelFactory(
    private val repository: DiscoveryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CollaborationDiscoveryViewModel(repository) as T
}
