package com.mohamedamr.devcollab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mohamedamr.devcollab.ui.theme.DevCollabTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mohamedamr.devcollab.core.settings.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as DevCollabApplication).appContainer
        setContent {
            val themeMode by appContainer.themePreferences.themeMode.collectAsStateWithLifecycle()
            var hasChosenAccess by remember {
                mutableStateOf(appContainer.welcomePreferences.isCompletedForCurrentInstall())
            }
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DevCollabTheme(darkTheme = darkTheme) {
                DevCollabApp(
                    developerRepository = appContainer.developerRepository,
                    authRepository = appContainer.authRepository,
                    appMemberRepository = appContainer.appMemberRepository,
                    discoveryRepository = appContainer.discoveryRepository,
                    collaborationRequestRepository = appContainer.collaborationRequestRepository,
                    savedDeveloperRepository = appContainer.savedDeveloperRepository,
                    themeMode = themeMode,
                    onThemeModeChanged = appContainer.themePreferences::setThemeMode,
                    hasChosenAccess = hasChosenAccess,
                    onWelcomeCompleted = {
                        appContainer.welcomePreferences.completeForCurrentInstall()
                        hasChosenAccess = true
                    },
                )
            }
        }
    }
}
