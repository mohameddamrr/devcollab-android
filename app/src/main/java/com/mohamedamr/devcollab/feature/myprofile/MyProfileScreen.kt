package com.mohamedamr.devcollab.feature.myprofile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mohamedamr.devcollab.R
import com.mohamedamr.devcollab.core.designsystem.FeaturePlaceholder

@Composable
fun MyProfileScreen(modifier: Modifier = Modifier) {
    FeaturePlaceholder(
        title = stringResource(R.string.profile_title),
        description = stringResource(R.string.profile_phase_one_description),
        modifier = modifier,
    )
}
