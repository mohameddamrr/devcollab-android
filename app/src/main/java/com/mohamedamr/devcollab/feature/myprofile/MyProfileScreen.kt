package com.mohamedamr.devcollab.feature.myprofile

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mohamedamr.devcollab.domain.repository.AuthRepository
import com.mohamedamr.devcollab.domain.repository.AppMemberRepository
import com.mohamedamr.devcollab.R
import coil3.compose.AsyncImage
import com.mohamedamr.devcollab.core.settings.ThemeMode

@Composable
fun MyProfileRoute(
    authRepository: AuthRepository,
    appMemberRepository: AppMemberRepository,
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository, appMemberRepository),
    )
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()
    MyProfileScreen(
        uiState = uiState,
        onSignIn = { activity?.let(authViewModel::signIn) },
        onSignOut = authViewModel::signOut,
        themeMode = themeMode,
        onThemeModeChanged = onThemeModeChanged,
        onEditProfile = authViewModel::startEditingProfile,
        onCancelEdit = authViewModel::cancelEditingProfile,
        onSaveProfile = authViewModel::saveProfile,
        onBioChange = authViewModel::updateBio,
        onLocationChange = authViewModel::updateLocation,
        onContactChange = authViewModel::updateContactMethod,
        onAvailableChange = authViewModel::setAvailable,
        onRemoteChange = authViewModel::setRemotePreferred,
        onInterestToggle = authViewModel::toggleInterest,
        onProjectTypeToggle = authViewModel::toggleProjectType,
        modifier = modifier,
    )
}

@Composable
fun MyProfileScreen(
    uiState: AuthUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChanged: (ThemeMode) -> Unit = {},
    onEditProfile: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onSaveProfile: () -> Unit = {},
    onBioChange: (String) -> Unit = {},
    onLocationChange: (String) -> Unit = {},
    onContactChange: (String) -> Unit = {},
    onAvailableChange: (Boolean) -> Unit = {},
    onRemoteChange: (Boolean) -> Unit = {},
    onInterestToggle: (String) -> Unit = {},
    onProjectTypeToggle: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.auth_profile_title), style = MaterialTheme.typography.headlineMedium)
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Appearance", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Choose how WeDevelop looks on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onThemeModeChanged(mode) },
                            label = {
                                Text(mode.name.lowercase().replaceFirstChar(Char::uppercase))
                            },
                        )
                    }
                }
            }
        }
        when {
            uiState.isCheckingSession -> CircularProgressIndicator()
            uiState.user == null -> {
                Text(stringResource(R.string.auth_signed_out_description))
                Button(onClick = onSignIn, enabled = !uiState.isSigningIn) {
                    Text(
                        if (uiState.isSigningIn) stringResource(R.string.auth_opening_github)
                        else stringResource(R.string.auth_sign_in),
                    )
                }
            }
            else -> {
                ElevatedCard {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AsyncImage(
                            model = uiState.user.photoUrl,
                            contentDescription = null,
                            modifier = Modifier.size(88.dp).clip(CircleShape),
                        )
                        Text(
                            uiState.user.displayName ?: stringResource(R.string.auth_github_member),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            stringResource(R.string.auth_github_id, uiState.user.githubUserId),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.auth_member_explanation),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (uiState.isCreatingMemberProfile) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(stringResource(R.string.auth_creating_member_profile))
                        } else if (uiState.memberProfile != null) {
                            Text(
                                stringResource(R.string.auth_member_profile_ready),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                uiState.memberProfile?.let { profile ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Collaboration profile", style = MaterialTheme.typography.titleLarge)
                            if (uiState.isEditingProfile) {
                                Text("Available for collaboration")
                                Switch(uiState.draftAvailable, onAvailableChange)
                                OutlinedTextField(
                                    value = uiState.draftBio,
                                    onValueChange = onBioChange,
                                    label = { Text("Collaboration bio") },
                                    supportingText = { Text("${uiState.draftBio.length}/500") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text("Technologies of interest")
                                Text(
                                    "These are your preferences, not verified skill claims.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                ChoiceChips(PROFILE_INTERESTS, uiState.draftInterests, onInterestToggle)
                                Text("Preferred project types")
                                ChoiceChips(PROJECT_TYPES, uiState.draftProjectTypes, onProjectTypeToggle)
                                Text("Prefer remote collaboration")
                                Switch(uiState.draftRemotePreferred, onRemoteChange)
                                OutlinedTextField(
                                    value = uiState.draftLocation,
                                    onValueChange = onLocationChange,
                                    label = { Text("Location (optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = uiState.draftContactMethod,
                                    onValueChange = onContactChange,
                                    label = { Text("Contact method (optional)") },
                                    supportingText = { Text("Only add information you intend to share after acceptance.") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                uiState.profileValidationMessage?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error)
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = onCancelEdit) { Text("Cancel") }
                                    Button(
                                        onClick = onSaveProfile,
                                        enabled = !uiState.isSavingProfile,
                                    ) {
                                        Text(if (uiState.isSavingProfile) "Saving…" else "Save profile")
                                    }
                                }
                            } else {
                                Text(if (profile.availableForCollaboration) "Available for collaboration" else "Not currently available")
                                if (profile.collaborationBio.isNotBlank()) Text(profile.collaborationBio)
                                if (profile.collaborationInterests.isNotEmpty()) Text("Technology interests: ${profile.collaborationInterests.joinToString()}")
                                if (profile.preferredProjectTypes.isNotEmpty()) Text("Projects: ${profile.preferredProjectTypes.joinToString()}")
                                Text(if (profile.remotePreferred) "Remote preferred" else "Location-based collaboration")
                                if (profile.location.isNotBlank()) Text("Location: ${profile.location}")
                                Button(onClick = onEditProfile) {
                                    Text(if (profile.onboardingCompleted) "Edit profile" else "Complete profile")
                                }
                            }
                        }
                    }
                }
                OutlinedButton(onClick = onSignOut) { Text(stringResource(R.string.auth_sign_out)) }
            }
        }
        uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun ChoiceChips(
    choices: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { choice ->
            FilterChip(
                selected = choice in selected,
                onClick = { onToggle(choice) },
                label = { Text(choice) },
            )
        }
    }
}

private val PROFILE_INTERESTS = listOf(
    "Kotlin", "Android", "Jetpack Compose", "Room", "Retrofit",
    "Java", "Python", "JavaScript", "React", "Firebase",
)
private val PROJECT_TYPES = listOf("Side project", "Open source", "Hackathon", "Startup", "Learning project")

private class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val appMemberRepository: AppMemberRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AuthViewModel(authRepository, appMemberRepository) as T
}

private tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
