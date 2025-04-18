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
        if (!BlockManager.isAccessibilityServiceInitialized()) {
            Log.d("Numbux", "⏳ Servicio aún no inicializado completamente. Ignorando evento.")
            return
        }

//        if (BlockManager.shouldSkipFirstEvent()) {
  //          BlockManager.markFirstEventHandled()
    //        Log.d("Numbux", "⛔ Ignorando primer evento tras iniciar accesibilidad")
      //      return
        //}

        val packageName = event.packageName?.toString() ?: return

        val defaultLauncher = getDefaultLauncherPackage(this)
        if (packageName == defaultLauncher) {
            Log.d("Numbux", "🚫 Ignorando evento del launcher ($packageName)")
            return
        }

        // ⏳ Bloquear eventos si el servicio aún no está listo
        if (!BlockManager.isAccessibilityServiceInitialized()) {
            Log.d("Numbux", "⏳ Servicio aún no inicializado completamente. Ignorando evento.")
            return
        }

        // 🎯 Solo procesar si es cambio de ventana (actividad visible)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d("Numbux", "⚠️ Evento ignorado por tipo: ${event.eventType}")
            return
        }

        // ✅ Si el usuario está en el launcher, limpiar los dismissed
        if (packageName == getDefaultLauncherPackage(this)) {
            Log.d("Numbux", "🏠 Usuario en el launcher, reseteando dismissedPackages")
            BlockManager.clearAllDismissed()
        }

        // ✅ Reinicia los dismiss si el usuario cambió de app
        if (packageName != lastPackage) {
            Log.d("Numbux", "📲 Cambio de app detectado: $lastPackage -> $packageName")

            // 🔄 Limpiar dismissed si se reabre la misma app
            if (BlockManager.isDismissed(packageName)) {
                Log.d("Numbux", "🔁 Reapertura detectada de $packageName, removiendo de dismissed")
                BlockManager.clearDismissed(packageName)
            }

            BlockManager.resetAllDismissedIfPackageChanged(packageName)
        }

        val className = event.className?.toString() ?: ""

        Log.d("Numbux", "🎯 className detectado: $className en $packageName")

        Log.d("Numbux", "📍 Clase detectada: $className")
        Log.d("Numbux", "🔍 Texto raíz: ${rootInActiveWindow?.text}")
        Log.d("Numbux", "🕵️ Evento detectado: ${event.eventType}, paquete: $packageName")
        Log.d("Numbux", "🚩 isAppBlocked($packageName) = ${BlockManager.isAppBlocked(packageName)}")
        Log.d("Numbux", "🔒 isShowingPin = ${BlockManager.isShowingPin}")

        if (esIntentoDesactivarAccesibilidad(className, packageName)) {
            Log.d("Numbux", "⚠️ Intento de desactivar accesibilidad detectado (AlertDialog)")

            val rootNode = rootInActiveWindow
            if (rootNode != null && !overlayVisible) {
                val textos = listOf("Desactivar", "Turn Off", "Disable")
                val nodes = textos.flatMap { rootNode.findAccessibilityNodeInfosByText(it) }

                if (nodes.isNotEmpty()) {
                    val node = nodes.first()
                    val bounds = android.graphics.Rect()
                    node.getBoundsInScreen(bounds)

                    Log.d("Numbux", "🛡️ Coordenadas botón Turn Off: $bounds")

                    val intent = Intent(this, OverlayBlockerService::class.java).apply {
                        putExtra("x", bounds.left)
                        putExtra("y", bounds.top)
                        putExtra("width", bounds.width())
                        putExtra("height", bounds.height())
                    }
                    startService(intent)
                    overlayVisible = true
                }
            }

            if (!BlockManager.isShowingPin) {
                BlockManager.isShowingPin = true
                val broadcast = Intent("com.example.numbux.SHOW_PIN").apply {
                    setPackage("com.example.numbux")
                    putExtra("app_package", "com.android.settings")
                }
                sendBroadcast(broadcast)

                Handler(Looper.getMainLooper()).postDelayed({
                    BlockManager.isShowingPin = false
                }, 6_000)
            }

            return
        }


        if (esPantallaDeDesinstalacion(className, packageName)) {
            Log.d("Numbux", "⚠️ Intento de desinstalación detectado: $className")

            if (!BlockManager.isShowingPin) {
                BlockManager.isShowingPin = true

                val rootNode = rootInActiveWindow

                // 🧪 Extra debug
                Log.d("Numbux", "📋 rootInActiveWindow = $rootNode")
                val uninstallNodes = rootNode?.findAccessibilityNodeInfosByText("Uninstall")
                val desinstalarNodes = rootNode?.findAccessibilityNodeInfosByText("Desinstalar")
                val eliminarNodes = rootNode?.findAccessibilityNodeInfosByText("Eliminar")
                val quitarNodes = rootNode?.findAccessibilityNodeInfosByText("Quitar")

                Log.d("Numbux", "🔍 Uninstall nodes found: ${uninstallNodes?.size}")
                Log.d("Numbux", "🔍 Desinstalar nodes found: ${desinstalarNodes?.size}")
                Log.d("Numbux", "🔍 Eliminar nodes found: ${eliminarNodes?.size}")
                Log.d("Numbux", "🔍 Quitar nodes found: ${quitarNodes?.size}")

                val uninstallNode = listOf("Uninstall", "Desinstalar", "Eliminar", "Quitar")
                    .flatMap { rootNode?.findAccessibilityNodeInfosByText(it) ?: emptyList() }
                    .firstOrNull()

                if (uninstallNode != null) {
                    val bounds = android.graphics.Rect()
                    uninstallNode.getBoundsInScreen(bounds)

                    val overlayIntent = Intent(this, OverlayBlockerService::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("x", bounds.left)
                        putExtra("y", bounds.top)
                        putExtra("width", bounds.width())
                        putExtra("height", bounds.height())
                    }
                    startService(overlayIntent)
                    Log.d("Numbux", "🛡️ Overlay lanzado sobre botón de desinstalación")
                } else {
                    Log.w("Numbux", "❌ No se encontró ningún botón de desinstalación")
                }

                // 🔐 Mostrar PIN con un pequeño delay para no chocar contra la transición de sistema
                Handler(Looper.getMainLooper()).postDelayed({
                    val pinIntent = Intent("com.example.numbux.SHOW_PIN").apply {
                        setPackage("com.example.numbux")
                        putExtra("app_package", packageName)
                    }
                    sendBroadcast(pinIntent)

                    Handler(Looper.getMainLooper()).postDelayed({
                        BlockManager.isShowingPin = false
                        stopService(Intent(this, OverlayBlockerService::class.java)) // limpia overlay
                        Log.d("Numbux", "⏱️ PIN y overlay reseteados")
                    }, 5_000)
                }, 300) // pequeño delay evita que el sistema ignore la actividad
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

    private fun esPantallaDeDesinstalacion(className: String?, packageName: String?): Boolean {
        return packageName == "com.android.packageinstaller" &&
                (className?.contains("Uninstall", ignoreCase = true) == true ||
                        className?.contains("Uninstaller", ignoreCase = true) == true)
    }

    private fun esIntentoDesactivarAccesibilidad(className: String?, packageName: String?): Boolean {
        return packageName == "com.android.settings" && className?.contains("AlertDialog", ignoreCase = true) == true
    }


}
