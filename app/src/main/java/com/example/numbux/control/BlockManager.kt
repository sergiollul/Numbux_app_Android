package com.example.numbux.control

import android.content.Context
import android.util.Log

object BlockManager {
    private val blockedApps = mutableSetOf<String>()
    private val temporarilyAllowed = mutableSetOf<String>()

    private val dismissedPackages = mutableSetOf<String>()  // 👈 NUEVO

    fun resetFirstEvent() {
        firstEventSkipped = false
    }

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

    private var accessibilityServiceInitialized = false

    private var firstEventSkipped = false

    fun shouldSkipFirstEvent(): Boolean = !firstEventSkipped

    fun markFirstEventHandled() {
        firstEventSkipped = true
    }

    fun markAccessibilityServiceInitialized() {
        accessibilityServiceInitialized = true
    }

    fun isAccessibilityServiceInitialized(): Boolean {
        return accessibilityServiceInitialized
    }

    // 🔁 Nueva lógica: solo ignora PIN mientras no cambie de app
    fun dismissUntilAppChanges(packageName: String) {
        dismissedPackages.add(packageName)
    }

    fun isDismissed(packageName: String): Boolean {
        return dismissedPackages.contains(packageName)
    }

    fun clearDismissed(packageName: String) {
        dismissedPackages.remove(packageName)
    }

    fun resetAllDismissedIfPackageChanged(currentPackage: String) {
        val iterator = dismissedPackages.iterator()
        while (iterator.hasNext()) {
            val pkg = iterator.next()
            if (pkg != currentPackage) {
                iterator.remove()
            }
        }
    }

    // ⏱️ Control de tiempo opcional (mantener por si se quiere usar luego)
    private var lastPinShownTime: Long = 0

    fun shouldShowPin(): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastPinShownTime) > 1000
    }

    fun markPinShown() {
        lastPinShownTime = System.currentTimeMillis()
    }

    fun clearAllDismissed() {
        dismissedPackages.clear()
    }

    // ⚡ Forzar evaluación de evento aunque no cambie de paquete
    private var forceEvaluateOnce = false

    fun shouldForceEvaluate(): Boolean {
        val should = forceEvaluateOnce
        forceEvaluateOnce = false
        return should
    }

    fun markForceEvaluateOnce() {
        forceEvaluateOnce = true
    }

}
