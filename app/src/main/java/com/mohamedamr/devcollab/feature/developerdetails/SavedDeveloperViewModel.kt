package com.mohamedamr.devcollab.feature.developerdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.repository.SavedDeveloperRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedDeveloperViewModel(private val repository: SavedDeveloperRepository) : ViewModel() {
    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()
    private var profile: DeveloperProfile? = null
    private var observation: Job? = null

    fun bind(profile: DeveloperProfile) {
        if (this.profile?.githubId == profile.githubId) return
        this.profile = profile
        observation?.cancel()
        observation = viewModelScope.launch { repository.observeIsSaved(profile.githubId).collect(_isSaved::emit) }
    }

    fun toggle() {
        val current = profile ?: return
        viewModelScope.launch { if (_isSaved.value) repository.remove(current.githubId) else repository.save(current) }
    }
}

class SavedDeveloperViewModelFactory(private val repository: SavedDeveloperRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = SavedDeveloperViewModel(repository) as T
}
