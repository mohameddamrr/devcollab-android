package com.mohamedamr.devcollab.feature.myprofile

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamedamr.devcollab.domain.repository.AuthRepository
import com.mohamedamr.devcollab.domain.repository.AppMemberRepository
import com.mohamedamr.devcollab.domain.model.AuthenticatedAppUser
import com.mohamedamr.devcollab.domain.model.AppMemberProfile
import com.mohamedamr.devcollab.domain.model.CollaborationProfileInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val appMemberRepository: AppMemberRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authenticatedUser
                .catch { error ->
                    _uiState.update {
                        it.copy(isCheckingSession = false, errorMessage = error.message)
                    }
                }
                .collectLatest { user ->
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            isSigningIn = false,
                            user = user,
                            memberProfile = if (user == null) null else it.memberProfile,
                            isCreatingMemberProfile = user != null,
                            errorMessage = null,
                        )
                    }
                    if (user != null) ensureAppMember(user)
                }
        }
    }

    private suspend fun ensureAppMember(user: AuthenticatedAppUser) {
        runCatching { appMemberRepository.ensureMember(user) }
            .onSuccess { memberProfile ->
                _uiState.update {
                    if (it.user?.firebaseUid != user.firebaseUid) it
                    else it.withMemberProfile(memberProfile).copy(isCreatingMemberProfile = false)
                }
            }
            .onFailure { error ->
                _uiState.update {
                    if (it.user?.firebaseUid != user.firebaseUid) it
                    else it.copy(
                        isCreatingMemberProfile = false,
                        errorMessage = error.message ?: "Could not create app member profile",
                    )
                }
            }
    }

    fun signIn(activity: ComponentActivity) {
        if (_uiState.value.isSigningIn) return
        _uiState.update { it.copy(isSigningIn = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { authRepository.signInWithGitHub(activity) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isSigningIn = false, errorMessage = error.message ?: "Sign-in failed")
                    }
                }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun startEditingProfile() {
        _uiState.update { state ->
            state.memberProfile?.let(state::withMemberProfile)?.copy(isEditingProfile = true) ?: state
        }
    }

    fun cancelEditingProfile() {
        _uiState.update { state ->
            state.memberProfile?.let(state::withMemberProfile)?.copy(isEditingProfile = false) ?: state
        }
    }

    fun updateBio(value: String) = updateDraft { copy(draftBio = value.take(BIO_MAX_LENGTH)) }
    fun updateLocation(value: String) = updateDraft { copy(draftLocation = value.take(LOCATION_MAX_LENGTH)) }
    fun updateContactMethod(value: String) = updateDraft { copy(draftContactMethod = value.take(CONTACT_MAX_LENGTH)) }
    fun setAvailable(value: Boolean) = updateDraft { copy(draftAvailable = value) }
    fun setRemotePreferred(value: Boolean) = updateDraft { copy(draftRemotePreferred = value) }
    fun toggleInterest(value: String) = updateDraft {
        copy(draftInterests = draftInterests.toggle(value))
    }
    fun toggleProjectType(value: String) = updateDraft {
        copy(draftProjectTypes = draftProjectTypes.toggle(value))
    }

    fun saveProfile() {
        val state = _uiState.value
        val user = state.user ?: return
        if (state.isSavingProfile) return
        val validationMessage = when {
            state.draftBio.length > BIO_MAX_LENGTH -> "Bio is too long"
            state.draftLocation.length > LOCATION_MAX_LENGTH -> "Location is too long"
            state.draftContactMethod.length > CONTACT_MAX_LENGTH -> "Contact method is too long"
            else -> null
        }
        if (validationMessage != null) {
            _uiState.update { it.copy(profileValidationMessage = validationMessage) }
            return
        }
        val input = CollaborationProfileInput(
            availableForCollaboration = state.draftAvailable,
            collaborationBio = state.draftBio,
            collaborationInterests = state.draftInterests.sorted(),
            preferredProjectTypes = state.draftProjectTypes.sorted(),
            remotePreferred = state.draftRemotePreferred,
            location = state.draftLocation,
            contactMethod = state.draftContactMethod,
        )
        _uiState.update { it.copy(isSavingProfile = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { appMemberRepository.updateCollaborationProfile(user.firebaseUid, input) }
                .onSuccess { profile ->
                    _uiState.update { current ->
                        if (current.user?.firebaseUid != user.firebaseUid) current
                        else current.withMemberProfile(profile).copy(
                            isSavingProfile = false,
                            isEditingProfile = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { current ->
                        if (current.user?.firebaseUid != user.firebaseUid) current
                        else current.copy(
                            isSavingProfile = false,
                            errorMessage = error.message ?: "Could not save collaboration profile",
                        )
                    }
                }
        }
    }

    private fun updateDraft(transform: AuthUiState.() -> AuthUiState) {
        _uiState.update { state -> state.transform().copy(profileValidationMessage = null) }
    }
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (value in this) this - value else this + value

private fun AuthUiState.withMemberProfile(profile: AppMemberProfile): AuthUiState = copy(
    memberProfile = profile,
    draftBio = profile.collaborationBio,
    draftAvailable = profile.availableForCollaboration,
    draftInterests = profile.collaborationInterests.toSet(),
    draftProjectTypes = profile.preferredProjectTypes.toSet(),
    draftRemotePreferred = profile.remotePreferred,
    draftLocation = profile.location,
    draftContactMethod = profile.contactMethod,
    profileValidationMessage = null,
)

private const val BIO_MAX_LENGTH = 500
private const val LOCATION_MAX_LENGTH = 100
private const val CONTACT_MAX_LENGTH = 200
