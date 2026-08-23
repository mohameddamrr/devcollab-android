package com.mohamedamr.devcollab.feature.discover

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import coil3.compose.AsyncImage
import com.mohamedamr.devcollab.R
import com.mohamedamr.devcollab.domain.model.DeveloperSummary
import com.mohamedamr.devcollab.domain.model.DeveloperAccountType
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import java.text.DateFormat
import java.util.Date

@Composable
fun DiscoverRoute(
    developerRepository: DeveloperRepository,
    onDeveloperClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchViewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(developerRepository),
    )
    val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()

    DiscoverScreen(
        uiState = uiState,
        pagingData = searchViewModel.pagingData,
        onQueryChanged = searchViewModel::onQueryChanged,
        onSearch = searchViewModel::search,
        onDeveloperClick = onDeveloperClick,
        modifier = modifier,
    )
}

@Composable
fun DiscoverScreen(
    uiState: SearchUiState,
    pagingData: Flow<PagingData<DeveloperSummary>>,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onDeveloperClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val developers = pagingData.collectAsLazyPagingItems()
    val submitSearch = {
        onSearch()
        keyboardController?.hide()
        Unit
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.discover_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.discover_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = uiState.query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SEARCH_FIELD_TAG),
            label = { Text(stringResource(R.string.search_developers_label)) },
            placeholder = { Text(stringResource(R.string.search_developers_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
            trailingIcon = {
                IconButton(
                    onClick = submitSearch,
                    enabled = developers.loadState.refresh !is LoadState.Loading,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_action),
                    )
                }
            },
        )
        Spacer(Modifier.height(16.dp))

        uiState.cachedAtEpochMillis?.let { cachedAt ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(
                        R.string.search_cached_results,
                        cachedAt.toDisplayDateTime(),
                    ),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        SearchResultContent(
            uiState = uiState,
            developers = developers,
            onDeveloperClick = onDeveloperClick,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun Long.toDisplayDateTime(): String = if (this > 0L) {
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(this))
} else {
    "—"
}

@Composable
private fun SearchResultContent(
    uiState: SearchUiState,
    developers: LazyPagingItems<DeveloperSummary>,
    onDeveloperClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val refreshState = developers.loadState.refresh
    when {
        uiState.validationError == SearchValidationError.EmptyQuery -> ErrorContent(
            reason = SearchErrorReason.EmptyQuery,
            onRetry = {},
            showRetry = false,
            modifier = modifier,
        )
        !uiState.hasSubmittedSearch -> MessageContent(
            message = stringResource(R.string.search_initial_message),
            modifier = modifier,
        )
        refreshState is LoadState.Loading -> {
            val loadingDescription = stringResource(R.string.search_loading)
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag(SEARCH_LOADING_TAG)
                    .semantics { contentDescription = loadingDescription },
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        refreshState is LoadState.Error -> ErrorContent(
            reason = refreshState.error.toSearchErrorReason(),
            onRetry = developers::retry,
            modifier = modifier,
        )
        developers.itemCount == 0 -> MessageContent(
            message = stringResource(R.string.search_empty_message),
            modifier = modifier,
        )
        else -> DeveloperResults(
            developers = developers,
            onDeveloperClick = onDeveloperClick,
            modifier = modifier,
        )
    }
}

@Composable
private fun DeveloperResults(
    developers: LazyPagingItems<DeveloperSummary>,
    onDeveloperClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().testTag(SEARCH_RESULTS_TAG),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = pluralStringResource(
                    R.plurals.search_loaded_count,
                    developers.itemCount,
                    developers.itemCount,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(
            count = developers.itemCount,
            key = developers.itemKey(DeveloperSummary::githubId),
        ) { index ->
            developers[index]?.let { developer ->
                DeveloperResultCard(
                    developer = developer,
                    onClick = { onDeveloperClick(developer.login) },
                )
            }
        }
        when (val appendState = developers.loadState.append) {
            is LoadState.Loading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
            is LoadState.Error -> item {
                ErrorContent(
                    reason = appendState.error.toSearchErrorReason(),
                    onRetry = developers::retry,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
            }
            is LoadState.NotLoading -> Unit
        }
    }
}

@Composable
private fun DeveloperResultCard(
    developer: DeveloperSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                AsyncImage(
                    model = developer.avatarUrl,
                    contentDescription = stringResource(
                        R.string.developer_avatar_description,
                        developer.login,
                    ),
                    modifier = Modifier.clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${developer.login}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.github_id_value, developer.githubId),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = developer.accountType.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (developer.isSiteAdmin) {
                    Text(
                        text = stringResource(R.string.github_site_admin),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    reason: SearchErrorReason,
    onRetry: () -> Unit,
    showRetry: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag(SEARCH_ERROR_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = reason.message(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        if (showRetry) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry_action))
            }
        }
    }
}

private fun Throwable.toSearchErrorReason(): SearchErrorReason {
    val repositoryError = (this as? com.mohamedamr.devcollab.domain.repository.DeveloperPagingException)
        ?.repositoryError
        ?: return SearchErrorReason.Unexpected
    return when (repositoryError) {
        com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError.NetworkUnavailable ->
            SearchErrorReason.NetworkUnavailable
        is com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError.RateLimited ->
            SearchErrorReason.RateLimited(repositoryError.resetAtEpochSeconds)
        is com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError.Server ->
            SearchErrorReason.Server(repositoryError.statusCode)
        com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError.InvalidData ->
            SearchErrorReason.InvalidData
        com.mohamedamr.devcollab.domain.repository.DeveloperRepositoryError.Unexpected ->
            SearchErrorReason.Unexpected
    }
}

@Composable
private fun MessageContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchErrorReason.message(): String = when (this) {
    SearchErrorReason.EmptyQuery -> stringResource(R.string.search_error_empty_query)
    SearchErrorReason.NetworkUnavailable -> stringResource(R.string.search_error_network)
    is SearchErrorReason.RateLimited -> stringResource(R.string.search_error_rate_limited)
    is SearchErrorReason.Server -> stringResource(R.string.search_error_server, statusCode)
    SearchErrorReason.InvalidData -> stringResource(R.string.search_error_invalid_data)
    SearchErrorReason.Unexpected -> stringResource(R.string.search_error_unexpected)
}

@Composable
private fun DeveloperAccountType.label(): String = when (this) {
    DeveloperAccountType.User -> stringResource(R.string.github_account_user)
    DeveloperAccountType.Organization -> stringResource(R.string.github_account_organization)
    DeveloperAccountType.Bot -> stringResource(R.string.github_account_bot)
    DeveloperAccountType.Unknown -> stringResource(R.string.github_account_unknown)
}

private class SearchViewModelFactory(
    private val developerRepository: DeveloperRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return SearchViewModel(developerRepository) as T
    }
}

internal const val SEARCH_FIELD_TAG = "search_field"
internal const val SEARCH_LOADING_TAG = "search_loading"
internal const val SEARCH_RESULTS_TAG = "search_results"
internal const val SEARCH_ERROR_TAG = "search_error"
