package com.example.numbux.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.accessibility.AccessibilityEvent
import androidx.preference.PreferenceManager
import com.example.numbux.control.BlockManager
import com.example.numbux.ui.PinActivity
import com.example.numbux.utils.getDefaultLauncherPackage
import android.widget.Toast
import android.graphics.Color

import android.provider.Settings
import android.net.Uri





class AppBlockerService : AccessibilityService() {

    private var recentsOverlay: View? = null

    private var inRecents = false
    private var touchBlocker: View? = null
    private var lastPackage: String? = null
    private lateinit var prefs: SharedPreferences

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
        if (key == "blocking_enabled") {
            val enabled = shared.getBoolean(key, true)
            Log.d("Numbux", "[DEBUG] blocking_enabled changed → $enabled")
            if (enabled) {
                BlockManager.clearAllTemporarilyAllowed()
                BlockManager.clearAllDismissed()
                lastPackage?.let { pkg ->
                    if (pkg != applicationContext.packageName
                        && pkg !in uninstallPackages
                        && !pkg.startsWith("com.android.systemui")
                        && !pkg.startsWith("com.android.settings")) {
                        Handler(Looper.getMainLooper()).post {
                            startActivity(
                                Intent(this, PinActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    putExtra("app_package", pkg)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var removeRunnable: Runnable? = null

    private fun showRecentsOverlay() {
        // 1) Cancel any scheduled removal
        removeRunnable?.let { handler.removeCallbacks(it) }

        // 2) If it’s already up, nothing to do
        if (recentsOverlay != null) return

        // 3) Check overlay permission
        if (!Settings.canDrawOverlays(this)) return

        // 4) Create & add the black view
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val overlay = View(this).apply {
            setBackgroundColor(Color.BLACK)
            setOnTouchListener { _, _ -> true }
        }
        val lp = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE
                    or LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        )
        wm.addView(overlay, lp)
        recentsOverlay = overlay
    }

    private fun scheduleRecentsClose() {
        // cancel any old removal
        removeRunnable?.let { handler.removeCallbacks(it) }

        // create a new one
        val r = Runnable {
            inRecents = false
            removeRecentsOverlay()
        }
        removeRunnable = r

        // only remove after 200 ms, giving Recents time to stabilize
        handler.postDelayed(r, 200)
    }


    private fun removeRecentsOverlay() {
        recentsOverlay?.let { view ->
            (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(view)
            recentsOverlay = null
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        prefs = PreferenceManager.getDefaultSharedPreferences(this).also { shared ->
            shared.registerOnSharedPreferenceChangeListener(prefListener)
        }
        Log.i("AppBlockerService", "🚀 Accessibility service CONNECTED, prefs initialized")

        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        Log.i("AppBlockerService", "••• serviceInfo.flags = ${serviceInfo.flags}")

        if (prefs.getBoolean("blocking_enabled", true)) {
            initializeBlockList()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString()   ?: return

        val isRecents = cls.contains("RecentsActivity", ignoreCase = true)
        val homePkg  = getDefaultLauncherPackage(this)

        if (!inRecents && isRecents) {
            // Entered Recents
            inRecents = true
            showRecentsOverlay()
        }
        else if (inRecents && !isRecents) {
            // Really left Recents (back to Home/Launcher)
            if (pkg == homePkg || (!pkg.startsWith("com.android.") && pkg != packageName)) {
                inRecents = false
                scheduleRecentsClose()
            }
        }
    }


    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN
            && event.keyCode == KeyEvent.KEYCODE_APP_SWITCH) {

            if (inRecents) {
                onRecentsClosed()
            } else {
                onRecentsOpened()
            }
            inRecents = !inRecents
        }
        return false
    }


    private fun onRecentsOpened() {
        Log.d("AppBlockerService", "✅ onRecentsPressed fired!")
        showRecentsOverlay()
    }

    private fun onRecentsClosed() {
        Log.d("AppBlockerService", "✅ Recents closed at ${System.currentTimeMillis()}")
        scheduleRecentsClose()
    }

    override fun onInterrupt() {
        // no-op
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }

    private fun blockAllTouchesFor(ms: Long) {
        if (touchBlocker != null) return
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            LayoutParams.TYPE_PHONE
        }
        touchBlocker = View(this).apply { setOnTouchListener { _, _ -> true } }
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
