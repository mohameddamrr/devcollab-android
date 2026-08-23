package com.mohamedamr.devcollab.app

import android.app.Application
import android.content.pm.ApplicationInfo

class DevCollabApplication : Application() {
    val appContainer: AppContainer by lazy {
        val isDebugBuild = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        AppContainer(
            applicationContext = applicationContext,
            isDebugBuild = isDebugBuild,
        )
    }
}
