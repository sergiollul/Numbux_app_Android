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
import com.example.numbux.ui.PinActivity
import com.example.numbux.utils.getDefaultLauncherPackage
import com.example.numbux.utils.getAllInstalledAppPackages
import com.example.numbux.overlay.OverlayBlockerService

class AppBlockerService : AccessibilityService() {
    private var lastPackage: String? = null
    private var overlayVisible = false

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        Log.d("Numbux", "🎯 className detectado: $className en $packageName")

        Log.d("Numbux", "📍 Clase detectada: $className")
        Log.d("Numbux", "🔍 Texto raíz: ${rootInActiveWindow?.text}")
        Log.d("Numbux", "🕵️ Evento detectado: ${event.eventType}, paquete: $packageName")
        Log.d("Numbux", "🚩 isAppBlocked($packageName) = ${BlockManager.isAppBlocked(packageName)}")
        Log.d("Numbux", "🔒 isShowingPin = ${BlockManager.isShowingPin}")

        // 👇 IGNORAR búsqueda con lupa en ajustes
        val clasesIgnoradas = listOf(
            "com.android.settings.intelligence.search.SearchActivity", // lupa nueva
            "SearchSettingsActivity", // algunas versiones usan este nombre
            "SettingsHomepageActivity" // inicio de Ajustes
        )

        if (clasesIgnoradas.any { className.contains(it, ignoreCase = true) }) {
            Log.d("Numbux", "🔍 Ignorando pantalla de sistema: $className")
            return
        }


        val accesoSensibles = listOf(
            "AccessibilitySettingsActivity",
            "AccessibilityDetailsSettingsActivity",
            "DeviceAdminSettingsActivity",
            "NotificationAccessSettingsActivity"
        )

        // Overlay si se abre pantalla sensible con Numbux
        if (packageName == "com.android.settings" && accesoSensibles.any { className.contains(it, true) }) {
            val rootNode = rootInActiveWindow ?: return
            val contieneNumbux = rootNode.text?.toString()?.contains("Numbux", true) == true
                    || hasNodeWithText(rootNode, "Numbux")

            Log.d("Numbux", "⚙️ Entrando en pantalla de ajustes sensibles: $className")

            if (contieneNumbux && !overlayVisible) {
                Log.d("Numbux", "🛡️ Mostrando overlay encima del botón 'Desactivar'")
                startService(Intent(this, OverlayBlockerService::class.java))
                overlayVisible = true
            }
        } else {
            if (overlayVisible) {
                Log.d("Numbux", "🫹 Ocultando overlay")
                stopService(Intent(this, OverlayBlockerService::class.java))
                overlayVisible = false
            }
        }

        // Fallback: si detectamos botón desactivar
        if (packageName == "com.android.settings" && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val rootNode = rootInActiveWindow ?: return
            val nodeList = mutableListOf<AccessibilityNodeInfo>()
            findNodesByText(rootNode, "Turn Off", nodeList)

            if (nodeList.isNotEmpty() && !BlockManager.isShowingPin) {
                Log.d("Numbux", "🚩 Botón 'Desactivar' detectado por fallback")

                BlockManager.isShowingPin = true
                performGlobalAction(GLOBAL_ACTION_BACK)

                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, PinActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("app_package", "com.android.settings")
                    }
                    startActivity(intent)
                }, 100)

                mostrarOverlaySobreBotonDesactivar()
                return
            }
        }

        // Bloqueo normal por apps
        if (packageName == lastPackage) return
        lastPackage = packageName

        if (BlockManager.isAppBlocked(packageName) && !BlockManager.isShowingPin) {
            Log.d("Numbux", "Bloqueada y mostrando PIN: $packageName")

            BlockManager.isShowingPin = true
            performGlobalAction(GLOBAL_ACTION_BACK)

            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, PinActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("app_package", packageName)
                }
                startActivity(intent)
            }, 100)
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            packageNames = null
        }
        this.serviceInfo = info

        Log.d("Numbux", "✅ Servicio conectado")

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
            Log.d("Numbux", "✅ Launcher detectado: $it")
            whitelist.add(it)
        }

        BlockManager.setBlockedAppsExcept(this, whitelist)
        Log.d("Numbux", "🛡️ Whitelist completa: $whitelist")

        val bloqueadas = BlockManager.getBlockedAppsDebug()
        Log.d("Numbux", "📋 Apps bloqueadas: $bloqueadas")
        Log.d("Numbux", "❌ ¿Search bloqueado?: ${bloqueadas.contains("com.android.settings.intelligence")}")
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

    private fun mostrarOverlaySobreBotonDesactivar() {
        Handler(Looper.getMainLooper()).postDelayed({
            val rootNode = rootInActiveWindow ?: return@postDelayed
            val textos = listOf("Desactivar", "Disable", "Turn off")
            val nodes = textos.flatMap { rootNode.findAccessibilityNodeInfosByText(it) }

            if (nodes.isEmpty()) {
                Log.w("Numbux", "❌ No se encontró nodo con texto 'Desactivar'")
                return@postDelayed
            }

            val node = nodes.first()
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)

            Log.d("Numbux", "📦 Coordenadas botón: $bounds")

            val intent = Intent(this, OverlayBlockerService::class.java).apply {
                putExtra("x", bounds.left)
                putExtra("y", bounds.top)
                putExtra("width", bounds.width())
                putExtra("height", bounds.height())
            }
            startService(intent)
        }, 500)
    }
}
