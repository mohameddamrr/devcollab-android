package com.mohamedamr.devcollab.core.settings

import android.content.Context

class WelcomePreferences(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun isCompletedForCurrentInstall(): Boolean =
        preferences.getLong(COMPLETED_INSTALL_TIME_KEY, -1L) == currentInstallTime()

    fun completeForCurrentInstall() {
        check(preferences.edit().putLong(COMPLETED_INSTALL_TIME_KEY, currentInstallTime()).commit()) {
            "Unable to save the welcome choice"
        }
    }

    private fun currentInstallTime(): Long =
        context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime

    private companion object {
        const val PREFERENCES_NAME = "wedevelop_welcome"
        const val COMPLETED_INSTALL_TIME_KEY = "completed_install_time"
    }
}
