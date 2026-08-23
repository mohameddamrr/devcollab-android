package com.mohamedamr.devcollab.app

import android.os.Bundle
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mohamedamr.devcollab.ui.theme.DevCollabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val isDebugBuild = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val appContainer = AppContainer(isDebugBuild = isDebugBuild)
        setContent {
            DevCollabTheme {
                DevCollabApp(developerRepository = appContainer.developerRepository)
            }
        }
    }
}
