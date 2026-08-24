package com.mohamedamr.devcollab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mohamedamr.devcollab.ui.theme.DevCollabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as DevCollabApplication).appContainer
        setContent {
            DevCollabTheme {
                DevCollabApp(
                    developerRepository = appContainer.developerRepository,
                    authRepository = appContainer.authRepository,
                    appMemberRepository = appContainer.appMemberRepository,
                    discoveryRepository = appContainer.discoveryRepository,
                )
            }
        }
    }
}
