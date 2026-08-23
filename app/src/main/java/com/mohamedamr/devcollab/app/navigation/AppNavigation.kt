package com.mohamedamr.devcollab.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mohamedamr.devcollab.feature.myprofile.MyProfileScreen
import com.mohamedamr.devcollab.feature.requests.RequestsScreen
import com.mohamedamr.devcollab.feature.saved.SavedScreen
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.feature.discover.DiscoverRoute

@Composable
fun AppNavigation(
    navController: NavHostController,
    contentPadding: PaddingValues,
    developerRepository: DeveloperRepository,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Discover.route,
        modifier = modifier.padding(contentPadding),
    ) {
        composable(AppDestination.Discover.route) {
            DiscoverRoute(developerRepository = developerRepository)
        }
        composable(AppDestination.Requests.route) { RequestsScreen() }
        composable(AppDestination.Saved.route) { SavedScreen() }
        composable(AppDestination.Profile.route) { MyProfileScreen() }
    }
}
