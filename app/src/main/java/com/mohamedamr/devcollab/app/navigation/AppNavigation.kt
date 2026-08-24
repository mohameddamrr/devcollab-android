package com.mohamedamr.devcollab.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.mohamedamr.devcollab.feature.myprofile.MyProfileRoute
import com.mohamedamr.devcollab.domain.repository.AuthRepository
import com.mohamedamr.devcollab.domain.repository.AppMemberRepository
import com.mohamedamr.devcollab.domain.repository.DiscoveryRepository
import com.mohamedamr.devcollab.domain.repository.CollaborationRequestRepository
import com.mohamedamr.devcollab.domain.repository.SavedDeveloperRepository
import com.mohamedamr.devcollab.feature.requests.RequestsScreen
import com.mohamedamr.devcollab.feature.saved.SavedScreen
import com.mohamedamr.devcollab.domain.repository.DeveloperRepository
import com.mohamedamr.devcollab.feature.discover.DiscoverRoute
import com.mohamedamr.devcollab.feature.developerdetails.DeveloperDetailsRoute
import com.mohamedamr.devcollab.core.settings.ThemeMode

@Composable
fun AppNavigation(
    navController: NavHostController,
    contentPadding: PaddingValues,
    developerRepository: DeveloperRepository,
    authRepository: AuthRepository,
    appMemberRepository: AppMemberRepository,
    discoveryRepository: DiscoveryRepository,
    collaborationRequestRepository: CollaborationRequestRepository,
    savedDeveloperRepository: SavedDeveloperRepository,
    themeMode: ThemeMode,
    onThemeModeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Discover.route,
        modifier = modifier.padding(contentPadding),
    ) {
        composable(AppDestination.Discover.route) {
            DiscoverRoute(
                developerRepository = developerRepository,
                discoveryRepository = discoveryRepository,
                onDeveloperClick = { username ->
                    navController.navigate(DeveloperDetailsDestination.createRoute(username))
                },
            )
        }
        composable(
            route = DeveloperDetailsDestination.route,
            arguments = listOf(
                navArgument(DeveloperDetailsDestination.usernameArgument) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val username = backStackEntry.arguments
                ?.getString(DeveloperDetailsDestination.usernameArgument)
                .orEmpty()
            DeveloperDetailsRoute(
                username = username,
                developerRepository = developerRepository,
                savedDeveloperRepository = savedDeveloperRepository,
                authRepository = authRepository,
                onCollaborate = { githubUserId ->
                    navController.navigate(CollaborationRequestDestination.createRoute(githubUserId))
                },
                onMyProfile = { navController.navigate(AppDestination.Profile.route) },
                onBack = navController::navigateUp,
            )
        }
        composable(AppDestination.Requests.route) {
            RequestsScreen(authRepository, collaborationRequestRepository, appMemberRepository, developerRepository)
        }
        composable(
            route = CollaborationRequestDestination.route,
            arguments = listOf(
                navArgument(CollaborationRequestDestination.githubUserIdArgument) {
                    type = NavType.LongType
                },
            ),
        ) { backStackEntry ->
            RequestsScreen(
                authRepository = authRepository,
                requestRepository = collaborationRequestRepository,
                appMemberRepository = appMemberRepository,
                developerRepository = developerRepository,
                initialReceiverGithubId = backStackEntry.arguments
                    ?.getLong(CollaborationRequestDestination.githubUserIdArgument),
            )
        }
        composable(AppDestination.Saved.route) {
            SavedScreen(
                repository = savedDeveloperRepository,
                onDeveloperClick = { username ->
                    navController.navigate(DeveloperDetailsDestination.createRoute(username))
                },
            )
        }
        composable(AppDestination.Profile.route) {
            MyProfileRoute(
                authRepository = authRepository,
                appMemberRepository = appMemberRepository,
                themeMode = themeMode,
                onThemeModeChanged = onThemeModeChanged,
            )
        }
    }
}
