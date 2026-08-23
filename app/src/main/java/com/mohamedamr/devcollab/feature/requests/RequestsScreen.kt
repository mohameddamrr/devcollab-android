package com.mohamedamr.devcollab.feature.requests

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mohamedamr.devcollab.R
import com.mohamedamr.devcollab.core.designsystem.FeaturePlaceholder

@Composable
fun RequestsScreen(modifier: Modifier = Modifier) {
    FeaturePlaceholder(
        title = stringResource(R.string.requests_title),
        description = stringResource(R.string.requests_phase_one_description),
        modifier = modifier,
    )
}
