package com.mohamedamr.devcollab.feature.discover

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mohamedamr.devcollab.R
import com.mohamedamr.devcollab.core.designsystem.FeaturePlaceholder

@Composable
fun DiscoverScreen(modifier: Modifier = Modifier) {
    FeaturePlaceholder(
        title = stringResource(R.string.discover_title),
        description = stringResource(R.string.discover_phase_one_description),
        modifier = modifier,
    )
}
