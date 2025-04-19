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
        // 1️⃣ Evita procesar eventos antes de estar listo
        if (!BlockManager.isAccessibilityServiceInitialized()) {
            Log.d("Numbux", "⏳ Servicio aún no inicializado completamente. Ignorando evento.")
            return
        }

        // 2️⃣ Obtiene el package y nombre de clase
        val packageName = event.packageName?.toString() ?: return
        val className   = event.className?.toString() ?: ""

        // 3️⃣ Ignora eventos del launcher
        getDefaultLauncherPackage(this)?.let { launcher ->
            if (packageName == launcher) {
                Log.d("Numbux", "🚫 Ignorando evento del launcher ($packageName)")
                return
            }
        }

        // 4️⃣ Solo procesar cambios de ventana
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d("Numbux", "⚠️ Evento ignorado por tipo: ${event.eventType}")
            return
        }

        // 🚦 Si el usuario ya desbloqueó esta app con PIN, omitimos cualquier bloqueo adicional,
        //      salvo que esté apareciendo la pantalla de desinstalación
        if (BlockManager.isTemporarilyAllowed(packageName)
            && !esPantallaDeDesinstalacion(className, packageName)
        ) {
            Log.d("Numbux", "✅ App $packageName desbloqueada temporalmente, omitiendo bloqueo")
            return
        }

        // 5️⃣ Limpia dismissed si volvemos al launcher
        if (packageName == getDefaultLauncherPackage(this)) {
            Log.d("Numbux", "🏠 Usuario en el launcher, reseteando dismissedPackages")
            BlockManager.clearAllDismissed()
        }

        // 6️⃣ Reinicia dismissed si cambió de app
        if (packageName != lastPackage) {
            Log.d("Numbux", "📲 Cambio de app detectado: $lastPackage -> $packageName")
            if (BlockManager.isDismissed(packageName)) {
                Log.d("Numbux", "🔁 Reapertura de $packageName, eliminando de dismissed")
                BlockManager.clearDismissed(packageName)
            }
            BlockManager.resetAllDismissedIfPackageChanged(packageName)
        }
        lastPackage = packageName

        // 7️⃣ Evita loops con PinActivity y con tu propia app
        if (className.contains("PinActivity", ignoreCase = true)
            || packageName == applicationContext.packageName
        ) {
            Log.d("Numbux", "🚫 Ignorando evento interno o de PIN")
            return
        }

        // 8️⃣ No mostrar PIN si ya rehusó antes
        if (BlockManager.isDismissed(packageName)) {
            Log.d("Numbux", "🚫 App $packageName fue rechazada antes. No mostramos PIN de nuevo.")
            return
        }

        // 9️⃣ Detección de diálogo de desinstalación
        if (esPantallaDeDesinstalacion(className, packageName)) {
            // 🔓 Si ya permitimos la desinstalación con PIN, saltarnos el prompt
            if (BlockManager.isTemporarilyAllowed(packageName)) {
                Log.d("Numbux", "🔓 Desinstalación de $packageName ya permitida, no pedimos PIN de nuevo")
                return
            }
            Log.d("Numbux", "⚠️ Intento de desinstalación detectado: $className")
            if (!BlockManager.isShowingPin) {
                mostrarOverlaySobreBotonDesactivar()
                Handler(Looper.getMainLooper()).postDelayed({
                    // 🚀 Primero quitamos el overlay para no dejar el área roja bloqueada
                    stopService(Intent(this@AppBlockerService, OverlayBlockerService::class.java))
                    overlayVisible = false

                    // 🔐 Ahora sí enviamos el PIN
                    val pinIntent = Intent("com.example.numbux.SHOW_PIN").apply {
                        // Asegúrate de usar tu propio packageName aquí
                        setPackage(applicationContext.packageName)
                        putExtra("app_package", packageName)
                    }
                    sendBroadcast(pinIntent)
                    BlockManager.isShowingPin = true

                    // ⏲️ Limpiamos la bandera de “mostrando PIN” al cabo de 1s
                    Handler(Looper.getMainLooper()).postDelayed({
                        BlockManager.isShowingPin = false
                    }, 1_000)
                }, 300)
            }
            return
        }

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
        if (packageName == "com.android.settings" && accesoSensibles.any {
                className.contains(
                    it,
                    true
                )
            }) {
            val rootNode = rootInActiveWindow ?: return
            val contieneNumbux = rootNode.text?.toString()?.contains("Numbux", true) == true
                    || hasNodeWithText(rootNode, "Numbux")

            Log.d("Numbux", "⚙️ Entrando en pantalla de ajustes sensibles: $className")

            if (contieneNumbux && !overlayVisible) {
                Log.d("Numbux", "🛡️ Mostrando overlay encima del botón 'Desactivar'")
                startService(Intent(this, OverlayBlockerService::class.java))
                overlayVisible = true
            }

            if (!BlockManager.isShowingPin) {
                Log.d("Numbux", "🔐 Mostrando PIN porque se intenta desactivar accesibilidad")

                BlockManager.isShowingPin = true
                val broadcast = Intent("com.example.numbux.SHOW_PIN").apply {
                    setPackage("com.example.numbux")
                    putExtra("app_package", "com.android.settings") // lo marcamos como si fuera settings
                }
                sendBroadcast(broadcast)

                Handler(Looper.getMainLooper()).postDelayed({
                    BlockManager.isShowingPin = false
                    Log.d("Numbux", "⏲️ Timeout: Reiniciando isShowingPin tras intento de desactivación")
                }, 5_000) // un poco más largo para asegurar que el usuario vea el PIN
            }

        } else {
            if (overlayVisible) {
                Log.d("Numbux", "🫹 Ocultando overlay")
                stopService(Intent(this, OverlayBlockerService::class.java))
                overlayVisible = false
            }
        }

        // Fallback: si detectamos botón desactivar
        // Fallback: si detectamos botón desactivar
        if (packageName == "com.android.settings" && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val rootNode = rootInActiveWindow ?: return
            val nodeList = mutableListOf<AccessibilityNodeInfo>()
            findNodesByText(rootNode, "Turn Off", nodeList)

            if (nodeList.isNotEmpty() && !BlockManager.isShowingPin) {
                Log.d("Numbux", "🚩 Botón 'Desactivar' detectado por fallback")

                BlockManager.isShowingPin = true

                // ❗Primero tapamos el botón
                mostrarOverlaySobreBotonDesactivar()

                // ❗Luego lanzamos el PIN
                val broadcast = Intent("com.example.numbux.SHOW_PIN").apply {
                    setPackage("com.example.numbux")
                    putExtra("app_package", "com.android.settings")
                }
                sendBroadcast(broadcast)

                // ❗Por seguridad, quitamos la bandera luego de unos segundos
                Handler(Looper.getMainLooper()).postDelayed({
                    BlockManager.isShowingPin = false
                }, 5_000)

                return
            }
        }


        // Bloqueo normal por apps
        // ⚠️ Si es el mismo paquete, pero el sistema no lanzó otro evento, evaluamos igualmente si la app visible cambió
        if (packageName == lastPackage && !BlockManager.shouldForceEvaluate()) {
            val currentTop = getTopAppPackage()
            if (currentTop != null && currentTop != lastPackage) {
                Log.d("Numbux", "🔁 La app visible ($currentTop) cambió aunque el paquete no: forzando reevaluación")
                BlockManager.markForceEvaluateOnce()
            } else {
                Log.d("Numbux", "⏭️ Mismo paquete y sin cambio visual, no evaluamos")
                return
            }
        }
        lastPackage = packageName

        // 🧪 Ignorar si es el PIN mostrándose (evita loops)
        if (className.contains("PinActivity", ignoreCase = true)) {
            Log.d("Numbux", "🚫 Ignorando evento de PinActivity")
            return
        }

        // ❌ No mostrar PIN si es nuestra propia app
        if (packageName == applicationContext.packageName) {
            Log.d("Numbux", "🚫 Ignorando evento de nuestra propia app")
            return
        }

        // 🛡️ Evita mostrar PIN si el usuario ya lo rechazó antes
        if (BlockManager.isDismissed(packageName)) {
            Log.d("Numbux", "🚫 App $packageName fue rechazada antes. No mostramos PIN de nuevo.")
            return
        }

        Log.d("Numbux", "🧪 Evaluando bloqueo para $packageName: isAppBlocked=${BlockManager.isAppBlocked(packageName)}")
        Log.d("Numbux", "🧪 Evaluando si debemos mostrar PIN para $packageName")

        Handler(Looper.getMainLooper()).postDelayed({
            val currentPackage = try {
                rootInActiveWindow?.packageName
            } catch (e: Exception) {
                Log.w("Numbux", "⚠️ Error leyendo rootInActiveWindow tras delay: ${e.message}")
                null
            }

            Log.d("Numbux", "📦 postDelayed → currentPackage=$currentPackage")
            Log.d("Numbux", "📦 postDelayed → expectedPackage=$packageName")
            Log.d("Numbux", "📦 postDelayed → isShowingPin=${BlockManager.isShowingPin}")
            Log.d("Numbux", "📦 postDelayed → isAppBlocked=${BlockManager.isAppBlocked(packageName)}")

            if (currentPackage == packageName && !BlockManager.isShowingPin && BlockManager.isAppBlocked(packageName)) {
                Log.d("Numbux", "✅ Condiciones cumplidas. Mostramos PIN para $packageName")
                BlockManager.isShowingPin = true
                val broadcast = Intent("com.example.numbux.SHOW_PIN").apply {
                    setPackage("com.example.numbux") // asegúrate de que sea tu propio paquete, no el de la app bloqueada
                    putExtra("app_package", packageName)
                }
                BlockManager.markPinShown()
                Log.d("Numbux", "📣 Enviando broadcast para mostrar PIN a $packageName desde ${applicationContext.packageName}")
                sendBroadcast(broadcast)

                Handler(Looper.getMainLooper()).postDelayed({
                    BlockManager.isShowingPin = false
                    Log.d("Numbux", "⏲️ Timeout: Reiniciando isShowingPin por seguridad")
                }, 1_000) // 10 segundos, puedes ajustar

            } else {
                Log.d("Numbux", "❌ Condiciones NO cumplidas. No se muestra PIN para $packageName")
            }
        }, 200)
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {

        super.onServiceConnected()
        Log.d("Numbux", "✅ Servicio de accesibilidad conectado")

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
                Log.d("Numbux", "✅ Launcher detectado: $it")
                whitelist.add(it)
            }

            whitelist.add("com.android.packageinstaller")
            whitelist.add("com.google.android.packageinstaller")
            Log.d("Numbux", "✅ Agregados packageinstaller a la whitelist")

            BlockManager.resetFirstEvent()
            BlockManager.setBlockedAppsExcept(this, whitelist)
            BlockManager.markAccessibilityServiceInitialized()
            Log.d("Numbux", "✅ Servicio inicializado completamente tras delay")

            // 👁️ Evaluamos manualmente la app activa usando ActivityManager
            val topApp = getTopAppPackage()
            if (!topApp.isNullOrEmpty()) {
                Log.d("Numbux", "🔥 Evaluando app visible al encender accesibilidad: $topApp")

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
        Handler(Looper.getMainLooper()).post {
            val rootNode = rootInActiveWindow ?: return@post

            val textos = listOf("Desactivar", "Disable", "Turn off")
            val nodes = textos.flatMap { rootNode.findAccessibilityNodeInfosByText(it) }

            if (nodes.isEmpty()) {
                Log.w("Numbux", "❌ No se encontró nodo con texto 'Desactivar'")
                return@post
            }

            val node = nodes.first()
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)

            Log.d("Numbux", "📦 Coordenadas botón: $bounds")

            val intent = Intent(this, OverlayBlockerService::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                putExtra("x", bounds.left)
                putExtra("y", bounds.top)
                putExtra("width", bounds.width())
                putExtra("height", bounds.height())
            }
            startService(intent)
        }
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