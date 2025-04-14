package com.example.numbux.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.numbux.ui.PinActivity
import com.example.numbux.control.BlockManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.numbux.utils.getDefaultLauncherPackage
import com.example.numbux.utils.getAllInstalledAppPackages



class AppBlockerService : AccessibilityService() {
    private var lastPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        Log.d("Numbux", "🕵️‍♂️ Evento detectado: ${event.eventType}, paquete: $packageName")
        Log.d("Numbux", "🛑 isAppBlocked($packageName) = ${BlockManager.isAppBlocked(packageName)}")
        Log.d("Numbux", "🔒 isShowingPin = ${BlockManager.isShowingPin}")

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

        // ✅ Confirmar ejecución
        Log.d("Numbux", "✅ Servicio conectado y ejecutando carga de apps bloqueadas")

        val whitelist = mutableListOf(
            packageName, // Tu propia app
            "com.android.settings",
            "com.android.systemui",
            "com.android.inputmethod.latin",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.inputmethod",
            "com.miui.securitycenter",
            "com.sec.android.app.launcher", // <-- explícito, por seguridad
            "com.samsung.android.spay" // 👈 ¡Agregado para evitar bloqueos en home!
        )

        val launcherPackage = getDefaultLauncherPackage(this)
        if (launcherPackage != null) {
            Log.d("Numbux", "✅ Launcher detectado correctamente: $launcherPackage")
            whitelist.add(launcherPackage)
        } else {
            Log.w("Numbux", "⚠️ No se detectó launcher. ¡Peligro de bloqueo!")
        }

        // 👇 Aplica el bloqueo a todas excepto la whitelist
        BlockManager.setBlockedAppsExcept(this, whitelist)

        val bloqueadas = BlockManager.getBlockedAppsDebug()
        Log.d("Numbux", "❌ ¿Launcher bloqueado?: ${bloqueadas.contains("com.sec.android.app.launcher")}")

    }

}
