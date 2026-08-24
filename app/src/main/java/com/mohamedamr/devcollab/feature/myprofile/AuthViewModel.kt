package com.mohamedamr.devcollab.feature.myprofile

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohamedamr.devcollab.domain.repository.AuthRepository
import com.mohamedamr.devcollab.domain.repository.AppMemberRepository
import com.mohamedamr.devcollab.domain.model.AuthenticatedAppUser
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
                    else it.copy(memberProfile = memberProfile, isCreatingMemberProfile = false)
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
}
