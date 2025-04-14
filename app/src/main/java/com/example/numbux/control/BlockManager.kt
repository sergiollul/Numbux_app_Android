package com.example.numbux.control

import android.content.Context
import android.util.Log

object BlockManager {
    private val blockedApps = mutableSetOf<String>()
    private val temporarilyAllowed = mutableSetOf<String>()

    fun isAppBlocked(packageName: String): Boolean {
        return blockedApps.contains(packageName) && !temporarilyAllowed.contains(packageName)
    }

    fun blockApp(packageName: String) {
        blockedApps.add(packageName)
    }

    fun unblockApp(packageName: String) {
        blockedApps.remove(packageName)
        temporarilyAllowed.remove(packageName)
    }

    fun allowTemporarily(packageName: String) {
        temporarilyAllowed.add(packageName)
    }

    fun getAllInstalledAppPackages(context: Context): List<String> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)
        return apps.map { it.packageName }
    }

    fun setBlockedApps(apps: List<String>) {
        blockedApps.clear()
        blockedApps.addAll(apps)
    }

    fun setBlockedAppsExcept(context: Context, whitelist: List<String>) {
        val allApps = getAllInstalledAppPackages(context)
        val appsToBlock = allApps.filterNot { whitelist.contains(it) }
        blockedApps.clear()
        blockedApps.addAll(appsToBlock)
        Log.d("Numbux", "🔐 setBlockedAppsExcept(): whitelist=$whitelist")
        Log.d("Numbux", "🔐 setBlockedAppsExcept(): appsToBlock=$appsToBlock")
    }

    fun getBlockedAppsDebug(): Set<String> {
        return blockedApps
    }

    var isShowingPin = false
}
