package com.mohamedamr.devcollab.feature.saved

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mohamedamr.devcollab.R
import com.mohamedamr.devcollab.core.designsystem.FeaturePlaceholder

@Composable
fun SavedScreen(modifier: Modifier = Modifier) {
    FeaturePlaceholder(
        title = stringResource(R.string.saved_title),
        description = stringResource(R.string.saved_phase_one_description),
        modifier = modifier,
    )
}
