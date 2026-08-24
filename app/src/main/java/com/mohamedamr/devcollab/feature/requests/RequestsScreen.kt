package com.mohamedamr.devcollab.feature.requests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohamedamr.devcollab.domain.model.*
import com.mohamedamr.devcollab.domain.repository.AuthRepository
import com.mohamedamr.devcollab.domain.repository.CollaborationRequestRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RequestsUiState(
    val currentUser: AuthenticatedAppUser? = null,
    val received: List<CollaborationRequest> = emptyList(),
    val sent: List<CollaborationRequest> = emptyList(),
    val showSent: Boolean = false,
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val showCreateForm: Boolean = false,
    val receiverUid: String = "",
    val receiverGithubId: String = "",
    val projectName: String = "",
    val projectDescription: String = "",
    val technologies: String = "",
    val collaborationType: String = "Side project",
    val neededRole: String = "",
    val expectedCommitment: String = "",
    val message: String = "",
    val evidenceReasons: String = "",
    val errorMessage: String? = null,
)

class RequestsViewModel(
    private val authRepository: AuthRepository,
    private val requestRepository: CollaborationRequestRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RequestsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authenticatedUser.collectLatest { user ->
                _uiState.update { it.copy(currentUser = user, isLoading = user != null) }
                if (user == null) return@collectLatest
                coroutineScope {
                    launch { requestRepository.observeReceived().collect { value -> _uiState.update { it.copy(received = value, isLoading = false) } } }
                    launch { requestRepository.observeSent().collect { value -> _uiState.update { it.copy(sent = value, isLoading = false) } } }
                }
            }
        }
    }

    fun setShowSent(value: Boolean) = _uiState.update { it.copy(showSent = value) }
    fun setShowCreateForm(value: Boolean) = _uiState.update { it.copy(showCreateForm = value, errorMessage = null) }
    fun updateReceiverUid(value: String) = update { copy(receiverUid = value) }
    fun updateReceiverGithubId(value: String) = update { copy(receiverGithubId = value.filter(Char::isDigit)) }
    fun updateProjectName(value: String) = update { copy(projectName = value.take(120)) }
    fun updateProjectDescription(value: String) = update { copy(projectDescription = value.take(2000)) }
    fun updateTechnologies(value: String) = update { copy(technologies = value) }
    fun updateCollaborationType(value: String) = update { copy(collaborationType = value.take(100)) }
    fun updateNeededRole(value: String) = update { copy(neededRole = value.take(120)) }
    fun updateExpectedCommitment(value: String) = update { copy(expectedCommitment = value.take(200)) }
    fun updateMessage(value: String) = update { copy(message = value.take(2000)) }
    fun updateEvidenceReasons(value: String) = update { copy(evidenceReasons = value) }

    fun send() {
        val state = _uiState.value
        val user = state.currentUser ?: return
        val receiverGithubId = state.receiverGithubId.toLongOrNull()
        if (state.receiverUid.isBlank() || receiverGithubId == null || state.projectName.isBlank() || state.neededRole.isBlank() || state.expectedCommitment.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Recipient and required project fields must be completed") }
            return
        }
        val draft = CollaborationRequestDraft(
            receiverUid = state.receiverUid.trim(), senderGithubUserId = user.githubUserId,
            receiverGithubUserId = receiverGithubId, projectName = state.projectName,
            projectDescription = state.projectDescription, technologies = state.technologies.csv(),
            collaborationType = state.collaborationType, neededRole = state.neededRole,
            expectedCommitment = state.expectedCommitment, message = state.message,
            evidenceReasons = state.evidenceReasons.lines().map(String::trim).filter(String::isNotBlank),
        )
        work { requestRepository.create(draft); _uiState.update { it.copy(showCreateForm = false, projectName = "") } }
    }

    fun accept(id: String) = work { requestRepository.accept(id) }
    fun decline(id: String) = work { requestRepository.decline(id) }
    fun cancel(id: String) = work { requestRepository.cancel(id) }

    private fun work(action: suspend () -> Unit) {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { action() }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Request operation failed") }
            }
            _uiState.update { it.copy(isWorking = false) }
        }
    }

    private fun update(change: RequestsUiState.() -> RequestsUiState) =
        _uiState.update { it.change().copy(errorMessage = null) }
}

@Composable
fun RequestsScreen(authRepository: AuthRepository, requestRepository: CollaborationRequestRepository, modifier: Modifier = Modifier) {
    val vm: RequestsViewModel = viewModel(factory = RequestsViewModelFactory(authRepository, requestRepository))
    val state by vm.uiState.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Collaboration requests", style = MaterialTheme.typography.headlineSmall)
        if (state.currentUser == null) { Text("Sign in with GitHub from Profile to send and receive requests."); return@Column }
        Row(Modifier.fillMaxWidth()) {
            TextButton({ vm.setShowSent(false) }, Modifier.weight(1f)) { Text("Received") }
            TextButton({ vm.setShowSent(true) }, Modifier.weight(1f)) { Text("Sent") }
        }
        Button({ vm.setShowCreateForm(!state.showCreateForm) }) { Text("New request") }
        if (state.showCreateForm) RequestForm(state, vm)
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.isLoading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        val requests = if (state.showSent) state.sent else state.received
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(requests, key = CollaborationRequest::id) { RequestCard(it, state.showSent, state.isWorking, vm) }
        }
    }
}

@Composable private fun RequestForm(s: RequestsUiState, vm: RequestsViewModel) {
    Text("Only registered DevCollab members can receive requests.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Field(s.receiverUid, vm::updateReceiverUid, "Recipient app UID")
    Field(s.receiverGithubId, vm::updateReceiverGithubId, "Recipient GitHub ID")
    Field(s.projectName, vm::updateProjectName, "Project name")
    Field(s.projectDescription, vm::updateProjectDescription, "Project description")
    Field(s.technologies, vm::updateTechnologies, "Technologies (comma-separated)")
    Field(s.collaborationType, vm::updateCollaborationType, "Collaboration type")
    Field(s.neededRole, vm::updateNeededRole, "Role/help needed")
    Field(s.expectedCommitment, vm::updateExpectedCommitment, "Expected commitment")
    Field(s.evidenceReasons, vm::updateEvidenceReasons, "Why this developer? One reason per line")
    Field(s.message, vm::updateMessage, "Personal message")
    Button(vm::send, enabled = !s.isWorking) { Text("Send request") }
}

@Composable private fun Field(value: String, onChange: (String) -> Unit, label: String) =
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth())

@Composable private fun RequestCard(request: CollaborationRequest, sent: Boolean, disabled: Boolean, vm: RequestsViewModel) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(request.projectName, style = MaterialTheme.typography.titleMedium)
            Text(request.neededRole)
            if (request.technologies.isNotEmpty()) Text(request.technologies.joinToString(" • "))
            Text(request.status.name.lowercase().replaceFirstChar(Char::uppercase))
            if (request.status == CollaborationRequestStatus.PENDING) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sent) OutlinedButton({ vm.cancel(request.id) }, enabled = !disabled) { Text("Cancel") }
                else { Button({ vm.accept(request.id) }, enabled = !disabled) { Text("Accept") }; OutlinedButton({ vm.decline(request.id) }, enabled = !disabled) { Text("Decline") } }
            }
        }
    }
}

private fun String.csv() = split(',').map(String::trim).filter(String::isNotBlank).distinct().take(20)

private class RequestsViewModelFactory(private val auth: AuthRepository, private val requests: CollaborationRequestRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = RequestsViewModel(auth, requests) as T
}
