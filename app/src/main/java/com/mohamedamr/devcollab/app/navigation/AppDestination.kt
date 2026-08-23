package com.mohamedamr.devcollab.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.mohamedamr.devcollab.R

sealed class AppDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    data object Discover : AppDestination(
        route = "discover",
        labelRes = R.string.navigation_discover,
        icon = Icons.Default.Search,
    )

    data object Requests : AppDestination(
        route = "requests",
        labelRes = R.string.navigation_requests,
        icon = Icons.Outlined.Email,
    )

    data object Saved : AppDestination(
        route = "saved",
        labelRes = R.string.navigation_saved,
        icon = Icons.Outlined.Star,
    )

    data object Profile : AppDestination(
        route = "profile",
        labelRes = R.string.navigation_profile,
        icon = Icons.Default.Person,
    )

    companion object {
        val topLevelDestinations = listOf(Discover, Requests, Saved, Profile)
    }
}
