package com.example.numbux.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.preference.PreferenceManager
import com.example.numbux.control.BlockManager
import com.example.numbux.utils.getDefaultLauncherPackage

class AppBlockerService : AccessibilityService() {
    private var lastPackage: String? = null
    private var lastKnownToggle: Boolean = false

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // only care about full window changes
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        // -- detect toggle changes and reload block list when switched ON/OFF --
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val isNowEnabled = prefs.getBoolean("blocking_enabled", false)
        if (isNowEnabled && !lastKnownToggle) {
            lastKnownToggle = true
            initializeBlockList()
        } else if (!isNowEnabled && lastKnownToggle) {
            lastKnownToggle = false
            BlockManager.clearAllTemporarilyAllowed()
            BlockManager.clearAllDismissed()
            getDefaultLauncherPackage(this)?.let {
                BlockManager.setBlockedAppsExcept(this, listOf(it))
            }
        }

        val className   = event.className?.toString() ?: ""
        val packageName = event.packageName?.toString() ?: return

        Log.d("Numbux", "Event → pkg=$packageName, cls=$className, type=${event.eventType}")

        // 1) ALWAYS protect the 'turn‑off‑accessibility' dialog
        if (esIntentoDesactivarAccesibilidad(className, packageName)) {
            if (!BlockManager.isTemporarilyAllowed(packageName)) {
                Handler(Looper.getMainLooper()).postDelayed({
                    sendPinBroadcast(packageName)
                }, 300)
            }
            return
        }

        // 2) ALWAYS protect the uninstall screen
        if (esPantallaDeDesinstalacion(className, packageName)) {
            if (!BlockManager.isTemporarilyAllowed(packageName)) {
                Handler(Looper.getMainLooper()).postDelayed({
                    sendPinBroadcast(packageName)
                }, 300)
            }
            return
        }

        // 3) Skip *all* Settings UIs (core and intelligence/search)
        if (packageName.startsWith("com.android.settings")) {
            return
        }

        // 4) Skip all the System UI overlays (status bar, nav bar, keyboard, etc.)
        if (packageName == "com.android.systemui") {
            return
        }

        // Skip Samsung payment/keyboard packages as well:
        if (packageName.startsWith("com.samsung.android.spay")
            || packageName.startsWith("com.samsung.android.honeyboard")) {
            return
        }



        // Now respect the on/off toggle for every other app
        if (!prefs.getBoolean("blocking_enabled", true)) {
            return
        }

        // Ignore the launcher itself
        getDefaultLauncherPackage(this)?.let { launcher ->
            if (packageName == launcher) return
        }

        // Skip if user has already entered PIN for this package
        if (BlockManager.isTemporarilyAllowed(packageName)) {
            return
        }

        // On launcher → clear dismissed packages so they re‑block next time
        getDefaultLauncherPackage(this)?.let { launcher ->
            if (packageName == launcher) {
                BlockManager.clearAllDismissed()
                return
            }
        }

        // If package changed, reset any per‑app dismissals
        if (packageName != lastPackage) {
            if (BlockManager.isDismissed(packageName)) {
                BlockManager.clearDismissed(packageName)
            }
            BlockManager.resetAllDismissedIfPackageChanged(packageName)
        }
        lastPackage = packageName

        // Never block your own PIN activity
        if (className.contains("PinActivity", ignoreCase = true)
            || packageName == applicationContext.packageName
        ) {
            return
        }

        // If user temporarily dismissed this app, skip it
        if (BlockManager.isDismissed(packageName)) {
            return
        }

        // Finally—delay then show PIN if it's a blocked app on screen
        Handler(Looper.getMainLooper()).postDelayed({
            val currentPackage = try {
                rootInActiveWindow?.packageName
            } catch (e: Exception) {
                Log.w("Numbux", "Error reading rootInActiveWindow: ${e.message}")
                null
            }

            if (currentPackage == packageName && BlockManager.isAppBlocked(packageName)) {
                val broadcast = Intent("com.example.numbux.SHOW_PIN").apply {
                    setPackage(applicationContext.packageName)
                    putExtra("app_package", packageName)
                }
                sendBroadcast(broadcast)
                BlockManager.markPinShown()
            }
        }, 200)
    }


    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        // only initialize if toggle is ON
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (!prefs.getBoolean("blocking_enabled", false)) return

        // configure service
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            packageNames = null
        }
        serviceInfo = info

        // initial block list
        initializeBlockList()
    }

    /** Build or rebuild the block-list from preferences and defaults */
    private fun initializeBlockList() {
        // reset
        BlockManager.clearAllTemporarilyAllowed()
        BlockManager.clearAllDismissed()
        BlockManager.resetFirstEvent()

        // build whitelist
        val whitelist = mutableListOf(
            applicationContext.packageName,
            "com.android.settings",
            "com.android.systemui",
            "com.android.inputmethod.latin",
            "com.google.android.inputmethod.latin"
        )
        getDefaultLauncherPackage(this)?.let { whitelist.add(it) }
        // allow installer UIs
        whitelist.addAll(uninstallPackages)

        BlockManager.setBlockedAppsExcept(this, whitelist)
        BlockManager.markAccessibilityServiceInitialized()
    }

    private fun sendPinBroadcast(appPackage: String) {
        val pinIntent = Intent("com.example.numbux.SHOW_PIN").apply {
            setPackage(applicationContext.packageName)
            putExtra("app_package", appPackage)
        }
        sendBroadcast(pinIntent)
        BlockManager.isShowingPin = true
    }

    private fun hasNodeWithText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) return true
        for (i in 0 until node.childCount) if (hasNodeWithText(node.getChild(i), text)) return true
        return false
    }

    private fun getTopAppPackage(): String? {
        val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        return am.appTasks.firstOrNull()?.taskInfo?.topActivity?.packageName
    }

    private val uninstallPackages = listOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller"
    )

    private fun esPantallaDeDesinstalacion(className: String?, packageName: String?): Boolean {
        return uninstallPackages.contains(packageName) &&
                (className?.contains("Uninstall", true) == true ||
                        className?.contains("Uninstaller", true) == true)
    }

    private fun esIntentoDesactivarAccesibilidad(className: String?, packageName: String?): Boolean {
        return packageName == "com.android.settings" &&
                className?.contains("AlertDialog", ignoreCase = true) == true
    }
}
