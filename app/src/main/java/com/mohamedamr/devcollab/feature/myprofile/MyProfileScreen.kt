package com.mohamedamr.devcollab.feature.myprofile

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ElevatedCard
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

@Composable
fun MyProfileRoute(
    authRepository: AuthRepository,
    appMemberRepository: AppMemberRepository,
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
        modifier = modifier,
    )
}

@Composable
fun MyProfileScreen(
    uiState: AuthUiState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.auth_profile_title), style = MaterialTheme.typography.headlineMedium)
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
                OutlinedButton(onClick = onSignOut) { Text(stringResource(R.string.auth_sign_out)) }
            }
        }
        uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

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
