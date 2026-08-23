package com.mohamedamr.devcollab.feature.developerdetails

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.mohamedamr.devcollab.R
import com.mohamedamr.devcollab.domain.model.DeveloperProfile
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository

@Composable
fun DeveloperDetailsRoute(
    username: String,
    developerRepository: DeveloperRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val detailsViewModel: DeveloperDetailsViewModel = viewModel(
        factory = DeveloperDetailsViewModelFactory(username, developerRepository),
    )
    val uiState by detailsViewModel.uiState.collectAsStateWithLifecycle()
    val shareText = when (val result = uiState.result) {
        is DeveloperDetailsResultUiState.Success -> stringResource(
            R.string.share_profile_text,
            result.profile.login,
            result.profile.profileUrl,
        )
        else -> ""
    }
    val shareChooserTitle = stringResource(R.string.share_profile_chooser_title)

    DeveloperDetailsScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = detailsViewModel::retry,
        onOpenGitHub = { profileUrl ->
            context.openGitHubProfile(profileUrl)
        },
        onShareProfile = {
            context.shareProfile(shareText, shareChooserTitle)
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperDetailsScreen(
    uiState: DeveloperDetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenGitHub: (String) -> Unit,
    onShareProfile: (DeveloperProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("@${uiState.username}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        when (val result = uiState.result) {
            DeveloperDetailsResultUiState.Loading -> DetailsLoading(
                modifier = Modifier.padding(contentPadding),
            )
            is DeveloperDetailsResultUiState.Error -> DetailsError(
                reason = result.reason,
                onRetry = onRetry,
                onBack = onBack,
                modifier = Modifier.padding(contentPadding),
            )
            is DeveloperDetailsResultUiState.Success -> DeveloperProfileContent(
                profile = result.profile,
                onOpenGitHub = onOpenGitHub,
                onShareProfile = onShareProfile,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

@Composable
private fun DeveloperProfileContent(
    profile: DeveloperProfile,
    onOpenGitHub: (String) -> Unit,
    onShareProfile: (DeveloperProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(DETAILS_SUCCESS_TAG),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(112.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = stringResource(
                            R.string.developer_avatar_description,
                            profile.login,
                        ),
                        modifier = Modifier.clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
                Spacer(Modifier.height(12.dp))
                profile.name?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    text = "@${profile.login}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.github_id_value, profile.githubId),
                    style = MaterialTheme.typography.bodyMedium,
                )
                profile.bio?.let { bio ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ProfileStat(
                    value = profile.publicRepositoryCount,
                    label = stringResource(R.string.profile_repositories),
                    modifier = Modifier.weight(1f),
                )
                ProfileStat(
                    value = profile.followers,
                    label = stringResource(R.string.profile_followers),
                    modifier = Modifier.weight(1f),
                )
                ProfileStat(
                    value = profile.following,
                    label = stringResource(R.string.profile_following),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onOpenGitHub(profile.profileUrl) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(R.string.open_github))
                }
                OutlinedButton(
                    onClick = { onShareProfile(profile) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Text(
                        text = stringResource(R.string.share_profile),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.profile_about),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        profile.company?.let { company ->
            item { ProfileField(stringResource(R.string.profile_company), company) }
        }
        profile.location?.let { location ->
            item { ProfileField(stringResource(R.string.profile_location), location) }
        }
        profile.websiteUrl?.let { website ->
            item { ProfileField(stringResource(R.string.profile_website), website) }
        }
        profile.publicEmail?.let { email ->
            item { ProfileField(stringResource(R.string.profile_public_email), email) }
        }
        profile.twitterUsername?.let { twitter ->
            item { ProfileField(stringResource(R.string.profile_twitter), "@$twitter") }
        }
        profile.isHireable?.let { isHireable ->
            item {
                ProfileField(
                    label = stringResource(R.string.profile_available_for_hire),
                    value = if (isHireable) {
                        stringResource(R.string.yes)
                    } else {
                        stringResource(R.string.no)
                    },
                )
            }
        }
        item {
            ProfileField(
                label = stringResource(R.string.profile_joined_github),
                value = profile.createdAt.substringBefore('T'),
            )
        }
        item {
            ProfileField(
                label = stringResource(R.string.profile_public_gists),
                value = profile.publicGistCount.toString(),
            )
        }
    }
}

@Composable
private fun ProfileStat(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
    }
}

@Composable
private fun DetailsLoading(modifier: Modifier = Modifier) {
    val loadingDescription = stringResource(R.string.profile_loading)
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(DETAILS_LOADING_TAG)
            .semantics { contentDescription = loadingDescription },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DetailsError(
    reason: DeveloperDetailsErrorReason,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canRetry = reason !is DeveloperDetailsErrorReason.InvalidUsername &&
        reason !is DeveloperDetailsErrorReason.NotFound
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag(DETAILS_ERROR_TAG),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = reason.message(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = if (canRetry) onRetry else onBack) {
            Text(
                stringResource(
                    if (canRetry) R.string.retry_action else R.string.navigate_back,
                ),
            )
        }
    }
}

private fun Context.openGitHubProfile(profileUrl: String) {
    val uri = profileUrl.toUri()
    val isGitHubWebUrl = uri.scheme.equals("https", ignoreCase = true) &&
        uri.host.equals("github.com", ignoreCase = true)
    if (!isGitHubWebUrl) return

    val intent = Intent(Intent.ACTION_VIEW, uri)
    if (intent.resolveActivity(packageManager) != null) {
        runCatching { startActivity(intent) }
    }
}

private fun Context.shareProfile(shareText: String, chooserTitle: String) {
    if (shareText.isBlank()) return

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val chooserIntent = Intent.createChooser(shareIntent, chooserTitle)
    if (chooserIntent.resolveActivity(packageManager) != null) {
        runCatching { startActivity(chooserIntent) }
    }
}

@Composable
private fun DeveloperDetailsErrorReason.message(): String = when (this) {
    DeveloperDetailsErrorReason.InvalidUsername -> stringResource(R.string.profile_error_invalid)
    DeveloperDetailsErrorReason.NotFound -> stringResource(R.string.profile_error_not_found)
    DeveloperDetailsErrorReason.NetworkUnavailable -> stringResource(R.string.search_error_network)
    is DeveloperDetailsErrorReason.RateLimited -> stringResource(R.string.search_error_rate_limited)
    is DeveloperDetailsErrorReason.Server -> stringResource(R.string.search_error_server, statusCode)
    DeveloperDetailsErrorReason.InvalidData -> stringResource(R.string.search_error_invalid_data)
    DeveloperDetailsErrorReason.Unexpected -> stringResource(R.string.search_error_unexpected)
}

private class DeveloperDetailsViewModelFactory(
    private val username: String,
    private val developerRepository: DeveloperRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DeveloperDetailsViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return DeveloperDetailsViewModel(username, developerRepository) as T
    }
}

internal const val DETAILS_LOADING_TAG = "developer_details_loading"
internal const val DETAILS_SUCCESS_TAG = "developer_details_success"
internal const val DETAILS_ERROR_TAG = "developer_details_error"
