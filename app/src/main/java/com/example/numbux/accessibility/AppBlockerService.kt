package com.example.numbux.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.accessibility.AccessibilityEvent
import androidx.preference.PreferenceManager
import com.example.numbux.control.BlockManager
import com.example.numbux.ui.PinActivity
import com.example.numbux.utils.getDefaultLauncherPackage

class AppBlockerService : AccessibilityService() {

    // Keep track of our overlay and last package seen
    private var touchBlocker: View? = null
    private var lastPackage: String? = null

    // SharedPreferences + listener to reset state when blocking is turned back on
    private lateinit var prefs: SharedPreferences
    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
            if (key == "blocking_enabled" && shared.getBoolean(key, true)) {
                Log.d("Numbux", "[DEBUG] blocking re-enabled → clearing state")
                BlockManager.clearAllTemporarilyAllowed()
                BlockManager.clearAllDismissed()
            }
        }

    override fun onServiceConnected() {
        super.onServiceConnected()

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (!prefs.getBoolean("blocking_enabled", true)) return

        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType        = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 0
            packageNames        = null
        }

        initializeBlockList()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg  = event.packageName?.toString() ?: return
        val cls  = event.className?.toString()   ?: ""
        val type = event.eventType

        // DEBUG: log every Settings event
        if (pkg.startsWith("com.android.settings")) {
            Log.d("Numbux", "⚙️ SETTINGS EVT: pkg=$pkg, cls=$cls, type=$type, texts=${event.text}")
        }

        // 1) Block uninstall dialog as soon as it opens
        if (pkg in uninstallPackages
            && !BlockManager.isTemporarilyAllowed(pkg)
            && type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val timeoutMs = 3000L
            Log.d("Numbux","[DEBUG] uninstall-state → block $timeoutMs")
            blockAllTouchesFor(timeoutMs)
        }

        // 2) Dismiss uninstall + show PIN
        if (pkg in uninstallPackages
            && !BlockManager.isTemporarilyAllowed(pkg)
            && (type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                type == AccessibilityEvent.TYPE_WINDOWS_CHANGED)
        ) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            // still not unlocked? show PIN again
            startActivity(
                Intent(this, PinActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("app_package", pkg)
                    }
            )
            return
        }

        // 3) PIN-protect turning Accessibility OFF until they unlock
        if (esIntentoDesactivarAccesibilidad(cls, pkg) &&
            !BlockManager.isTemporarilyAllowed(pkg) &&
            type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            val timeoutMs = 3000L
            Log.d("Numbux","[DEBUG] disable-acc-state → block $timeoutMs")
            blockAllTouchesFor(timeoutMs)

            Log.d("Numbux","[DEBUG] disable-acc-state → BACK")
            performGlobalAction(GLOBAL_ACTION_BACK)

            Log.d("Numbux","[DEBUG] disable-acc-state → PIN")
            startActivity(Intent(this, PinActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("app_package", pkg)
            })
            return
        }

        // 4) Now safe to skip Settings UI
        if (pkg.startsWith("com.android.settings")) return

        // 5) Skip System UI & Samsung overlays
        if (pkg == "com.android.systemui" ||
            pkg.startsWith("com.samsung.android.spay") ||
            pkg.startsWith("com.samsung.android.honeyboard")
        ) return

        // 6) Respect the master toggle
        if (!prefs.getBoolean("blocking_enabled", true)) return

        // 7) Ignore launcher itself
        getDefaultLauncherPackage(this)?.let { launcher ->
            if (pkg == launcher) return
        }

        // 8) If they’ve already unlocked this app, don’t PIN again
        if (BlockManager.isTemporarilyAllowed(pkg)) return

        // 9) On returning to launcher, clear manual dismissals
        getDefaultLauncherPackage(this)?.let { launcher ->
            if (pkg == launcher) {
                BlockManager.clearAllDismissed()
                return
            }
        }

        // 10) Reset per-app dismissals when switching apps
        if (pkg != lastPackage) {
            if (BlockManager.isDismissed(pkg)) {
                BlockManager.clearDismissed(pkg)
            }
            BlockManager.resetAllDismissedIfPackageChanged(pkg)
        }
        lastPackage = pkg

        // 11) Never block your own PIN Activity
        if (cls.contains("PinActivity", ignoreCase = true) ||
            pkg == applicationContext.packageName
        ) return

        // 12) Skip apps the user manually dismissed
        if (BlockManager.isDismissed(pkg)) return

        // 13) Finally: show PIN for blocked apps
        Handler(Looper.getMainLooper()).postDelayed({
            val current = runCatching { rootInActiveWindow?.packageName }.getOrNull()
            if (current == pkg && BlockManager.isAppBlocked(pkg)
                && !BlockManager.isTemporarilyAllowed(pkg)
            ) {
                sendPinBroadcast(pkg)
            }
        }, 200)
    }

    override fun onInterrupt() { /* no-op */ }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    // ─────────────────────────────────────────────────────────────────
    // Core helper: overlay a full-screen view that eats all touches
    private fun blockAllTouchesFor(ms: Long) {
        if (touchBlocker != null) return

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            LayoutParams.TYPE_PHONE
        }

        touchBlocker = View(this).apply {
            setOnTouchListener { _, _ -> true }
        }
        val lp = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            windowType,
            LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        wm.addView(touchBlocker, lp)
        Handler(Looper.getMainLooper()).postDelayed({
            touchBlocker?.let {
                wm.removeView(it)
                touchBlocker = null
            }
        }, ms)
    }

    // ─────────────────────────────────────────────────────────────────
    private fun initializeBlockList() {
        BlockManager.clearAllTemporarilyAllowed()
        BlockManager.clearAllDismissed()
        BlockManager.resetFirstEvent()

        val whitelist = mutableListOf<String>().apply {
            add(applicationContext.packageName)
            add("com.android.settings")
            add("com.android.systemui")
            add("com.android.inputmethod.latin")
            add("com.google.android.inputmethod.latin")
            getDefaultLauncherPackage(this@AppBlockerService)?.let { add(it) }
            addAll(uninstallPackages)
        }
        BlockManager.setBlockedAppsExcept(this, whitelist)
        BlockManager.markAccessibilityServiceInitialized()
    }

    private fun sendPinBroadcast(appPkg: String) {
        Intent("com.example.numbux.SHOW_PIN").also { intent ->
            intent.setPackage(applicationContext.packageName)
            intent.putExtra("app_package", appPkg)
            sendBroadcast(intent)
        }
        BlockManager.isShowingPin = true
    }

    private val uninstallPackages = listOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller"
    )

    private fun esIntentoDesactivarAccesibilidad(className: String?, pkg: String?): Boolean {
        return pkg == "com.android.settings" &&
                className?.contains("AlertDialog", ignoreCase = true) == true
    }
}