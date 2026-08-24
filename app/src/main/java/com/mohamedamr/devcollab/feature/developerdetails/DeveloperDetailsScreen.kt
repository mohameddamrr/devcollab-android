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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
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
import com.mohamedamr.devcollab.domain.model.DeveloperRepositorySummary
import com.mohamedamr.devcollab.domain.model.summarizePrimaryRepositoryLanguages
import com.mohamedamr.devcollab.domain.model.DeveloperActivity
import com.mohamedamr.devcollab.domain.model.DeveloperActivityKind
import com.mohamedamr.devcollab.domain.model.mostActiveRecently
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.domain.repository.SavedDeveloperRepository
import com.mohamedamr.devcollab.domain.repository.AuthRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.Locale

@Composable
fun DeveloperDetailsRoute(
    username: String,
    developerRepository: DeveloperRepository,
    savedDeveloperRepository: SavedDeveloperRepository,
    authRepository: AuthRepository,
    onCollaborate: (Long) -> Unit,
    onMyProfile: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val detailsViewModel: DeveloperDetailsViewModel = viewModel(
        factory = DeveloperDetailsViewModelFactory(username, developerRepository),
    )
    val uiState by detailsViewModel.uiState.collectAsStateWithLifecycle()
    val savedViewModel: SavedDeveloperViewModel = viewModel(
        factory = SavedDeveloperViewModelFactory(savedDeveloperRepository),
    )
    val isSaved by savedViewModel.isSaved.collectAsStateWithLifecycle()
    val currentUser by authRepository.authenticatedUser.collectAsStateWithLifecycle(initialValue = null)
    LaunchedEffect(uiState.result) {
        (uiState.result as? DeveloperDetailsResultUiState.Success)?.profile?.let(savedViewModel::bind)
    }
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
        onRetryRepositories = detailsViewModel::retryRepositories,
        onRetryActivity = detailsViewModel::retryActivity,
        onSelectTab = detailsViewModel::selectTab,
        onLoadMoreRepositories = detailsViewModel::loadMoreRepositories,
        onRepositoryQueryChanged = detailsViewModel::onRepositoryQueryChanged,
        onRepositorySortChanged = detailsViewModel::onRepositorySortChanged,
        onRefresh = detailsViewModel::refresh,
        onOpenGitHub = { profileUrl ->
            context.openGitHubUrl(profileUrl)
        },
        onOpenRepository = { repositoryUrl ->
            context.openGitHubUrl(repositoryUrl)
        },
        onShareProfile = {
            context.shareProfile(shareText, shareChooserTitle)
        },
        isSaved = isSaved,
        onToggleSaved = savedViewModel::toggle,
        currentGithubUserId = currentUser?.githubUserId,
        onCollaborate = onCollaborate,
        onMyProfile = onMyProfile,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperDetailsScreen(
    uiState: DeveloperDetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRetryRepositories: () -> Unit = {},
    onRetryActivity: () -> Unit = {},
    onSelectTab: (DeveloperProfileTab) -> Unit = {},
    onLoadMoreRepositories: () -> Unit = {},
    onRepositoryQueryChanged: (String) -> Unit = {},
    onRepositorySortChanged: (RepositorySort) -> Unit = {},
    onRefresh: () -> Unit = {},
    onOpenGitHub: (String) -> Unit,
    onOpenRepository: (String) -> Unit = {},
    onShareProfile: (DeveloperProfile) -> Unit,
    isSaved: Boolean = false,
    onToggleSaved: () -> Unit = {},
    currentGithubUserId: Long? = null,
    onCollaborate: (Long) -> Unit = {},
    onMyProfile: () -> Unit = {},
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
                isSaved = isSaved,
                onToggleSaved = onToggleSaved,
                currentGithubUserId = currentGithubUserId,
                onCollaborate = onCollaborate,
                onMyProfile = onMyProfile,
                selectedTab = uiState.selectedTab,
                repositoriesState = uiState.repositories,
                activityState = uiState.activity,
                onSelectTab = onSelectTab,
                onRetryRepositories = onRetryRepositories,
                onRetryActivity = onRetryActivity,
                onOpenRepository = onOpenRepository,
                repositoryQuery = uiState.repositoryQuery,
                repositorySort = uiState.repositorySort,
                onLoadMoreRepositories = onLoadMoreRepositories,
                onRepositoryQueryChanged = onRepositoryQueryChanged,
                onRepositorySortChanged = onRepositorySortChanged,
                isRefreshing = uiState.isRefreshing,
                refreshError = uiState.refreshError,
                onRefresh = onRefresh,
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
    isSaved: Boolean,
    onToggleSaved: () -> Unit,
    currentGithubUserId: Long?,
    onCollaborate: (Long) -> Unit,
    onMyProfile: () -> Unit,
    selectedTab: DeveloperProfileTab,
    repositoriesState: DeveloperRepositoriesUiState,
    activityState: DeveloperActivityUiState,
    onSelectTab: (DeveloperProfileTab) -> Unit,
    onRetryRepositories: () -> Unit,
    onRetryActivity: () -> Unit,
    onOpenRepository: (String) -> Unit,
    repositoryQuery: String,
    repositorySort: RepositorySort,
    onLoadMoreRepositories: () -> Unit,
    onRepositoryQueryChanged: (String) -> Unit,
    onRepositorySortChanged: (RepositorySort) -> Unit,
    isRefreshing: Boolean,
    refreshError: DeveloperDetailsErrorReason?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val repositorySuccess = repositoriesState as? DeveloperRepositoriesUiState.Success
    val visibleRepositories = repositorySuccess
        ?.repositories
        .orEmpty()
        .filter { repository ->
            repositoryQuery.isBlank() ||
                repository.name.contains(repositoryQuery.trim(), ignoreCase = true) ||
                repository.description?.contains(repositoryQuery.trim(), ignoreCase = true) == true ||
                repository.primaryLanguage?.contains(repositoryQuery.trim(), ignoreCase = true) == true
        }
        .sortedFor(repositorySort)
    val languageSummary = summarizePrimaryRepositoryLanguages(
        repositorySuccess?.repositories.orEmpty(),
    )

    LaunchedEffect(selectedTab, repositoryQuery, listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            layoutInfo.totalItemsCount > 0 &&
                lastVisibleIndex >= layoutInfo.totalItemsCount - REPOSITORY_PREFETCH_ITEMS
        }
            .distinctUntilChanged()
            .collect { isNearEnd ->
                if (
                    isNearEnd &&
                    selectedTab == DeveloperProfileTab.Repositories &&
                    repositoryQuery.isBlank()
                ) {
                    onLoadMoreRepositories()
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
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
            OutlinedButton(onClick = onToggleSaved, modifier = Modifier.fillMaxWidth()) {
                Text(if (isSaved) "Remove from Saved" else "Save developer")
            }
        }

        currentGithubUserId?.let { signedInGithubId ->
            item {
                Button(
                    onClick = {
                        if (signedInGithubId == profile.githubId) onMyProfile()
                        else onCollaborate(profile.githubId)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (signedInGithubId == profile.githubId) "View My Profile" else "Send Collaboration Request")
                }
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

        refreshError?.let { reason ->
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(DETAILS_REFRESH_ERROR_TAG),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = stringResource(
                            R.string.profile_refresh_failed,
                            reason.message(),
                        ),
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        item {
            PrimaryTabRow(selectedTabIndex = selectedTab.ordinal) {
                DeveloperProfileTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        text = {
                            Text(
                                stringResource(
                                    when (tab) {
                                        DeveloperProfileTab.Overview -> R.string.profile_tab_overview
                                        DeveloperProfileTab.Repositories -> {
                                            R.string.profile_tab_repositories
                                        }
                                        DeveloperProfileTab.Activity -> R.string.profile_tab_activity
                                    },
                                ),
                            )
                        },
                    )
                }
            }
        }

        when (selectedTab) {
            DeveloperProfileTab.Overview -> {
                item {
                    Text(
                        text = stringResource(R.string.profile_about),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (languageSummary.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.repository_languages_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(
                                    R.string.repository_languages_explanation,
                                    repositorySuccess?.repositories?.size ?: 0,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(
                        items = languageSummary,
                        key = { it.language },
                    ) { language ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(language.language)
                            Text(
                                stringResource(
                                    R.string.repository_language_value,
                                    language.repositoryCount,
                                    language.percentageOfLoadedRepositories,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
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

            DeveloperProfileTab.Repositories -> when (repositoriesState) {
                DeveloperRepositoriesUiState.Loading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag(REPOSITORIES_LOADING_TAG),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is DeveloperRepositoriesUiState.Error -> item {
                    RepositoryError(
                        reason = repositoriesState.reason,
                        onRetry = onRetryRepositories,
                    )
                }
                is DeveloperRepositoriesUiState.Success -> {
                    if (repositoriesState.repositories.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.repositories_empty),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                                    .testTag(REPOSITORIES_EMPTY_TAG),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = repositoryQuery,
                                    onValueChange = onRepositoryQueryChanged,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag(REPOSITORY_SEARCH_TAG),
                                    label = { Text(stringResource(R.string.repositories_search)) },
                                    singleLine = true,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RepositorySort.entries.forEach { sort ->
                                        FilterChip(
                                            selected = repositorySort == sort,
                                            onClick = { onRepositorySortChanged(sort) },
                                            label = { Text(sort.label()) },
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(R.string.repositories_loaded_scope),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (visibleRepositories.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.repositories_filter_empty),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = repositorySort.heading(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            items(
                                items = visibleRepositories,
                                key = DeveloperRepositorySummary::githubId,
                            ) { repository ->
                                RepositoryCard(
                                    repository = repository,
                                    onClick = { onOpenRepository(repository.repositoryUrl) },
                                )
                            }
                        }

                        if (repositoriesState.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                        .testTag(REPOSITORIES_APPEND_LOADING_TAG),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else if (repositoriesState.appendError != null) {
                            item {
                                RepositoryError(
                                    reason = repositoriesState.appendError,
                                    onRetry = onRetryRepositories,
                                )
                            }
                        } else if (
                            repositoryQuery.isNotBlank() && repositoriesState.canLoadMore
                        ) {
                            item {
                                OutlinedButton(
                                    onClick = onLoadMoreRepositories,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(stringResource(R.string.repositories_load_more))
                                }
                            }
                        }
                    }
                }
            }

            DeveloperProfileTab.Activity -> when (activityState) {
                DeveloperActivityUiState.Loading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag(ACTIVITY_LOADING_TAG),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is DeveloperActivityUiState.Error -> item {
                    RepositoryError(
                        reason = activityState.reason,
                        onRetry = onRetryActivity,
                    )
                }
                is DeveloperActivityUiState.Success -> {
                    if (activityState.activities.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.activity_empty),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                                    .testTag(ACTIVITY_EMPTY_TAG),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        val activeRepositories = mostActiveRecently(activityState.activities)
                        item {
                            Text(
                                text = stringResource(R.string.activity_recent_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        items(
                            items = activityState.activities,
                            key = DeveloperActivity::eventId,
                        ) { activity ->
                            ActivityCard(activity)
                        }
                        item {
                            MostActiveRecentlyCard(
                                repositories = activeRepositories.take(5),
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun MostActiveRecentlyCard(
    repositories: List<com.mohamedamr.devcollab.domain.model.RecentlyActiveRepository>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.activity_most_active_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.activity_most_active_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                repositories.forEachIndexed { index, repository ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Text(
                                text = "${index + 1}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = repository.repositoryName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Text(
                                text = pluralStringResource(
                                    R.plurals.activity_count,
                                    repository.activityCount,
                                    repository.activityCount,
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    if (index < repositories.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun RepositoryCard(
    repository: DeveloperRepositorySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag(REPOSITORY_CARD_TAG_PREFIX + repository.githubId),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = repository.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            repository.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repository.primaryLanguage?.let { language ->
                    Text(language, style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    text = stringResource(R.string.repository_stars, repository.starCount),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = stringResource(R.string.repository_forks, repository.forkCount),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = stringResource(
                    R.string.repository_updated,
                    repository.updatedAt.substringBefore('T'),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (repository.isArchived) {
                Text(
                    text = stringResource(R.string.repository_archived),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun ActivityCard(
    activity: DeveloperActivity,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = activity.description(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = activity.repositoryName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = activity.createdAt.substringBefore('T'),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeveloperActivity.description(): String = when (kind) {
    DeveloperActivityKind.Push -> if (commitCount != null) {
        stringResource(R.string.activity_pushed_commits, commitCount)
    } else {
        stringResource(R.string.activity_pushed)
    }
    DeveloperActivityKind.PullRequest -> stringResource(
        R.string.activity_pull_request,
        action ?: stringResource(R.string.activity_updated),
    )
    DeveloperActivityKind.Issue -> stringResource(
        R.string.activity_issue,
        action ?: stringResource(R.string.activity_updated),
    )
    DeveloperActivityKind.Create -> stringResource(R.string.activity_created)
    DeveloperActivityKind.Fork -> stringResource(R.string.activity_forked)
    DeveloperActivityKind.Watch -> stringResource(R.string.activity_starred)
    DeveloperActivityKind.Release -> stringResource(R.string.activity_release)
    DeveloperActivityKind.Other -> stringResource(
        R.string.activity_other,
        rawEventType.removeSuffix("Event"),
    )
}

@Composable
private fun RepositoryError(
    reason: DeveloperDetailsErrorReason,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .testTag(REPOSITORIES_ERROR_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = reason.message(),
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry_action))
        }
    }
}

private fun List<DeveloperRepositorySummary>.sortedFor(
    sort: RepositorySort,
): List<DeveloperRepositorySummary> = when (sort) {
    RepositorySort.RecentlyUpdated -> sortedWith(
        compareByDescending<DeveloperRepositorySummary> { it.updatedAt }
            .thenBy { it.githubId },
    )
    RepositorySort.Popular -> sortedWith(
        compareByDescending<DeveloperRepositorySummary> { it.starCount }
            .thenByDescending { it.forkCount }
            .thenByDescending { it.updatedAt }
            .thenBy { it.githubId },
    )
    RepositorySort.Name -> sortedWith(
        compareBy<DeveloperRepositorySummary> { it.name.lowercase(Locale.ROOT) }
            .thenBy { it.githubId },
    )
}

@Composable
private fun RepositorySort.label(): String = stringResource(
    when (this) {
        RepositorySort.RecentlyUpdated -> R.string.repository_sort_recent
        RepositorySort.Popular -> R.string.repository_sort_popular
        RepositorySort.Name -> R.string.repository_sort_name
    },
)

@Composable
private fun RepositorySort.heading(): String = stringResource(
    when (this) {
        RepositorySort.RecentlyUpdated -> R.string.repositories_recently_updated
        RepositorySort.Popular -> R.string.repositories_popular_loaded
        RepositorySort.Name -> R.string.repositories_name_loaded
    },
)

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

private fun Context.openGitHubUrl(gitHubUrl: String) {
    val uri = gitHubUrl.toUri()
    val isGitHubWebUrl = uri.scheme.equals("https", ignoreCase = true) &&
        uri.host.equals("github.com", ignoreCase = true)
    if (!isGitHubWebUrl) return

    val intent = Intent(Intent.ACTION_VIEW, uri)
    runCatching { startActivity(intent) }
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
internal const val DETAILS_REFRESH_ERROR_TAG = "developer_details_refresh_error"
internal const val REPOSITORIES_LOADING_TAG = "developer_repositories_loading"
internal const val REPOSITORIES_EMPTY_TAG = "developer_repositories_empty"
internal const val REPOSITORIES_ERROR_TAG = "developer_repositories_error"
internal const val REPOSITORY_CARD_TAG_PREFIX = "developer_repository_"
internal const val REPOSITORY_SEARCH_TAG = "repository_search"
internal const val REPOSITORIES_APPEND_LOADING_TAG = "developer_repositories_append_loading"
internal const val ACTIVITY_LOADING_TAG = "developer_activity_loading"
internal const val ACTIVITY_EMPTY_TAG = "developer_activity_empty"
private const val REPOSITORY_PREFETCH_ITEMS = 3
