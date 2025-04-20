package com.example.numbux.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.numbux.control.BlockManager
import com.example.numbux.utils.getDefaultLauncherPackage
import androidx.preference.PreferenceManager

class AppBlockerService : AccessibilityService() {
    private var lastPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        val className   = event.className?.toString() ?: ""
        val packageName = event.packageName?.toString() ?: return

        if (esIntentoDesactivarAccesibilidad(className, packageName)) {
           if (BlockManager.isTemporarilyAllowed("com.android.settings")) {
               return
           }
            Handler(Looper.getMainLooper()).postDelayed({
                sendPinBroadcast("com.android.settings")
            }, 300)
            return
        }

        if (esPantallaDeDesinstalacion(className, packageName)) {
            if (BlockManager.isTemporarilyAllowed("com.android.settings")) {
                return
            }
            Handler(Looper.getMainLooper()).postDelayed({
                sendPinBroadcast(packageName)
            }, 300)
            return
        }

        val enabled = PreferenceManager
            .getDefaultSharedPreferences(this)
            .getBoolean("blocking_enabled", true)
        if (!enabled) return

        getDefaultLauncherPackage(this)?.let { launcher ->
            if (packageName == launcher) {
                return
            }
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        if (BlockManager.isTemporarilyAllowed(packageName)
            && !esPantallaDeDesinstalacion(className, packageName)
        ) {
            return
        }

        if (packageName == getDefaultLauncherPackage(this)) {
            BlockManager.clearAllDismissed()
        }

        if (packageName != lastPackage) {
            if (BlockManager.isDismissed(packageName)) {
                BlockManager.clearDismissed(packageName)
            }
            BlockManager.resetAllDismissedIfPackageChanged(packageName)
        }
        lastPackage = packageName

        if (className.contains("PinActivity", ignoreCase = true)
            || packageName == applicationContext.packageName
        ) {
            return
        }

        if (BlockManager.isDismissed(packageName)) {
            return
        }

        val clasesIgnoradas = listOf(
            "com.android.settings.intelligence.search.SearchActivity", // lupa nueva
            "SearchSettingsActivity", // algunas versiones usan este nombre
            "SettingsHomepageActivity" // inicio de Ajustes
        )

        if (clasesIgnoradas.any { className.contains(it, ignoreCase = true) }) {
            return
        }

        val accesoSensibles = listOf(
            "AccessibilitySettingsActivity",
            "AccessibilityDetailsSettingsActivity",
            "DeviceAdminSettingsActivity",
            "NotificationAccessSettingsActivity"
        )

        if (packageName == "com.android.settings" && accesoSensibles.any {
                className.contains(
                    it,
                    true
                )
            }) {
            val rootNode = rootInActiveWindow ?: return
            val contieneNumbux = rootNode.text?.toString()?.contains("Numbux", true) == true
                    || hasNodeWithText(rootNode, "Numbux")

            if (!BlockManager.isShowingPin) {

                BlockManager.isShowingPin = true
                val broadcast = Intent("com.example.numbux.SHOW_PIN").apply {
                    setPackage("com.example.numbux")
                    putExtra("app_package", "com.android.settings") // lo marcamos como si fuera settings
                }
                sendBroadcast(broadcast)

                Handler(Looper.getMainLooper()).postDelayed({
                    BlockManager.isShowingPin = false
                }, 5_000) // un poco más largo para asegurar que el usuario vea el PIN
            }
        }

        if (packageName == "com.android.settings" && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val rootNode = rootInActiveWindow ?: return
            val nodeList = mutableListOf<AccessibilityNodeInfo>()
            findNodesByText(rootNode, "Turn Off", nodeList)

            if (nodeList.isNotEmpty() && !BlockManager.isShowingPin) {

                BlockManager.isShowingPin = true

                val broadcast = Intent("com.example.numbux.SHOW_PIN").apply {
                    setPackage("com.example.numbux")
                    putExtra("app_package", "com.android.settings")
                }
                sendBroadcast(broadcast)

                Handler(Looper.getMainLooper()).postDelayed({
                    BlockManager.isShowingPin = false
                }, 5_000)

                return
            }
        }

        if (packageName == lastPackage && !BlockManager.shouldForceEvaluate()) {
            val currentTop = getTopAppPackage()
            if (currentTop != null && currentTop != lastPackage) {
                BlockManager.markForceEvaluateOnce()
            } else {
                return
            }
        }
        lastPackage = packageName

        if (className.contains("PinActivity", ignoreCase = true)) {
            return
        }

        if (packageName == applicationContext.packageName) {
            return
        }

        if (BlockManager.isDismissed(packageName)) {
            return
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val currentPackage = try {
                rootInActiveWindow?.packageName
            } catch (e: Exception) {
                Log.w("Numbux", "⚠️ Error leyendo rootInActiveWindow tras delay: ${e.message}")
                null
            }

            if (currentPackage == packageName && !BlockManager.isShowingPin && BlockManager.isAppBlocked(packageName)) {
                BlockManager.isShowingPin = true
                val broadcast = Intent("com.example.numbux.SHOW_PIN").apply {
                    setPackage("com.example.numbux") // asegúrate de que sea tu propio paquete, no el de la app bloqueada
                    putExtra("app_package", packageName)
                }
                BlockManager.markPinShown()
                sendBroadcast(broadcast)

                Handler(Looper.getMainLooper()).postDelayed({
                    BlockManager.isShowingPin = false
                }, 1_000) // 10 segundos, puedes ajustar

            } else {
            }
        }, 200)
    }

    override fun onInterrupt() {}
    
    private fun sendPinBroadcast(appPackage: String) {
        val pinIntent = Intent("com.example.numbux.SHOW_PIN").apply {
            setPackage(applicationContext.packageName)
            putExtra("app_package", appPackage)
        }
        sendBroadcast(pinIntent)
        BlockManager.isShowingPin = true
    }

    override fun onServiceConnected() {

        super.onServiceConnected()

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            packageNames = null
        }
        this.serviceInfo = info

        val whitelist = mutableListOf(
            packageName,
            "com.android.settings",
            "com.android.systemui",
            "com.android.inputmethod.latin",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.inputmethod",        // teclado Samsung viejo
            "com.samsung.android.honeyboard",         // ✅ este es el nuevo, HONEYBOARD
            "com.miui.securitycenter",
            "com.sec.android.app.launcher",
            "com.samsung.android.spay",
            "com.android.settings.intelligence"
        )

        getDefaultLauncherPackage(this)?.let {
            whitelist.add(it)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val whitelist = mutableListOf(
                packageName,
                "com.android.settings",
                "com.android.systemui",
                "com.android.inputmethod.latin",
                "com.google.android.inputmethod.latin",
                "com.samsung.android.inputmethod",
                "com.samsung.android.honeyboard",
                "com.miui.securitycenter",
                "com.sec.android.app.launcher",
                "com.samsung.android.spay",
                "com.android.settings.intelligence"
            )

            getDefaultLauncherPackage(this)?.let {
                whitelist.add(it)
            }

            whitelist.add("com.android.packageinstaller")
            whitelist.add("com.google.android.packageinstaller")

            BlockManager.resetFirstEvent()
            BlockManager.setBlockedAppsExcept(this, whitelist)
            BlockManager.markAccessibilityServiceInitialized()

            val topApp = getTopAppPackage()
            if (!topApp.isNullOrEmpty()) {

                BlockManager.markForceEvaluateOnce()

                val fakeEvent = AccessibilityEvent.obtain().apply {
                    eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    packageName = topApp
                    className = "" // opcional
                }

                onAccessibilityEvent(fakeEvent)
            } else {
                Log.w("Numbux", "⚠️ No se pudo determinar la app activa al activar accesibilidad")
            }

        }, 300)

        val bloqueadas = BlockManager.getBlockedAppsDebug()
    }

    private fun findNodesByText(node: AccessibilityNodeInfo?, text: String, result: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) result.add(node)
        for (i in 0 until node.childCount) findNodesByText(node.getChild(i), text, result)
    }

    private fun hasNodeWithText(node: AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) return true
        for (i in 0 until node.childCount) if (hasNodeWithText(node.getChild(i), text)) return true
        return false
    }

    private fun getTopAppPackage(): String? {
        val am = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val tasks = am.appTasks
        return tasks.firstOrNull()
            ?.taskInfo
            ?.topActivity
            ?.packageName
    }

    private val uninstallPackages = listOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller"    // Android 12+ uses this for permission/uninstall UI
    )

    private fun esPantallaDeDesinstalacion(className: String?, packageName: String?): Boolean {
        return uninstallPackages.contains(packageName) &&
                (className?.contains("Uninstall", true) == true ||
                        className?.contains("Uninstaller", true) == true)
    }

    private fun esIntentoDesactivarAccesibilidad(className: String?, packageName: String?): Boolean {
        return packageName == "com.android.settings" && className?.contains("AlertDialog", ignoreCase = true) == true
    }
}