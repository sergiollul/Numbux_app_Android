package com.example.numbux.utils

import android.content.Context
import android.provider.Settings

object AccessibilityUtils {
    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val expected = "${context.packageName}/com.example.numbux.accessibility.AppBlockerService"
        return enabledServices?.contains(expected) == true
    }
}