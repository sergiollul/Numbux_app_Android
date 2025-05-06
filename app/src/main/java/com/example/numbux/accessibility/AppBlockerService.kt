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

import android.view.Gravity
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent.*


class AppBlockerService : AccessibilityService() {

    private var recentsOverlay: View? = null

    private var inRecents = false
    private var touchBlocker: View? = null
    private var lastPackage: String? = null
    private lateinit var prefs: SharedPreferences

    private val prefListener =
        SharedPreferences.OnSharedPreferenceChangeListener { shared, key ->
            if (key != "blocking_enabled") return@OnSharedPreferenceChangeListener
            val enabled = shared.getBoolean(key, true)
            Log.d("AppBlockerService", "[DEBUG] blocking_enabled → $enabled")

            if (enabled) {
                // 1) Wipe any old allows/dismissals & rebuild your blocklist
                BlockManager.clearAllTemporarilyAllowed()
                BlockManager.clearAllDismissed()
                initializeBlockList()

                // 2) If there’s a real 3rd-party app in front, PIN-lock it immediately
                val current = rootInActiveWindow?.packageName?.toString()
                val homePkg = getDefaultLauncherPackage(this)
                if (current != null
                    && current != applicationContext.packageName
                    && current != homePkg
                    && current !in uninstallPackages
                    && !current.startsWith("com.android.systemui")
                    && !current.startsWith("com.android.settings")
                ) {
                    Handler(Looper.getMainLooper()).post {
                        startActivity(
                            Intent(this, PinActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra("app_package", current)
                            }
                        )
                    }
                }
            } else {
                // 3) Tearing off: remove any Recents overlay or touch‐blocker you showed
                recentsOverlay?.let {
                    (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
                    recentsOverlay = null
                }
                touchBlocker?.let {
                    (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
                    touchBlocker = null
                }
                // 4) And clear any inRecents flag so overlay logic won’t fire until real recents
                inRecents = false
            }
        }


    private val handler = Handler(Looper.getMainLooper())
    private var removeRunnable: Runnable? = null

    private fun showRecentsOverlay() {
        // only when the toggle is on
        if (!prefs.getBoolean("blocking_enabled", true)) return

        // 1) Cancel any scheduled removal
        removeRunnable?.let { handler.removeCallbacks(it) }

        // 2) If it’s already up, nothing to do
        if (recentsOverlay != null) return

        // 3) Check overlay permission
        if (!Settings.canDrawOverlays(this)) return

        // 4) Get nav-bar height
        val res = resources
        val navBarId = res.getIdentifier("navigation_bar_height", "dimen", "android")
        val navBarH = if (navBarId > 0) res.getDimensionPixelSize(navBarId) else 0

        // 5) Convert 100 dp into pixels for your extra bottom gap
        val bottomGapDp = 100f
        val displayMetrics = res.displayMetrics
        val bottomGapPx = (bottomGapDp * displayMetrics.density).toInt()

        // 6) Compute overlay height = full screen minus nav-bar minus extra gap
        val screenH = displayMetrics.heightPixels
        val overlayH = screenH - navBarH - bottomGapPx

        // 7) Build & add the view just as before
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val overlay = View(this).apply {
            setBackgroundColor(Color.BLACK)
            setOnTouchListener { _, _ -> true }
        }
        val lp = LayoutParams(
            LayoutParams.MATCH_PARENT,
            overlayH,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE
                    or LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP
        }
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

        prefs = PreferenceManager
            .getDefaultSharedPreferences(this)
            .also { sp ->
                sp.registerOnSharedPreferenceChangeListener(prefListener)
            }
        Log.i("AppBlockerService","…CONNECTED")

        // Now enforce whatever the toggle currently says:
        if (prefs.getBoolean("blocking_enabled", true)) {
            initializeBlockList()
        } else {
            // ensure no stray overlays/touch blockers
            recentsOverlay = null
            touchBlocker   = null
            inRecents      = false
        }

        // 2) Set up key-event filtering
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        Log.i("AppBlockerService", "••• serviceInfo.flags = ${serviceInfo.flags}")

        // 3) Enforce whatever the toggle currently says
        val enabled = prefs.getBoolean("blocking_enabled", true)
        if (enabled) {
            // Build or rebuild your block list
            initializeBlockList()
        } else {
            // Tear down any Recents overlay or touch-blocker
            recentsOverlay?.let {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
                recentsOverlay = null
            }
            touchBlocker?.let {
                (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)
                touchBlocker = null
            }
            // Reset Recents flag so overlay logic won't fire accidentally
            inRecents = false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val type = event.eventType
        if (type != TYPE_WINDOW_STATE_CHANGED &&
            type != TYPE_WINDOW_CONTENT_CHANGED &&
            type != TYPE_WINDOWS_CHANGED) return

        // Common vars
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: ""
        val homePkg = getDefaultLauncherPackage(this)
        val isRecents = cls.contains("RecentsActivity", ignoreCase = true)
        // Get your app’s visible name at runtime:
        val appName = applicationContext
            .applicationInfo
            .loadLabel(packageManager)
            .toString()

        // ————— Recents overlay logic (only when toggle ON) —————
        if (prefs.getBoolean("blocking_enabled", true) && type == TYPE_WINDOW_STATE_CHANGED) {
            if (!inRecents && isRecents) {
                inRecents = true
                showRecentsOverlay()
                return
            }
            if (inRecents && !isRecents &&
                (pkg == homePkg || (!pkg.startsWith("com.android.") && pkg != applicationContext.packageName))
            ) {
                inRecents = false
                scheduleRecentsClose()
                return
            }
        }

        // ————— Always-on SYSTEM flows —————

        // 1) PIN-protect turning Accessibility OFF
        if (pkg == "com.android.settings"
            && cls.contains("AlertDialog", ignoreCase = true)
            && !BlockManager.isTemporarilyAllowed(pkg)
            && (type == TYPE_WINDOW_STATE_CHANGED
                    || type == TYPE_WINDOW_CONTENT_CHANGED
                    || type == TYPE_WINDOWS_CHANGED)
        ) {
            blockAllTouchesFor(3_000)
            performGlobalAction(GLOBAL_ACTION_BACK)
            startActivity(Intent(this, PinActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("app_package", pkg)
            })
            return
        }

        // ————— Uninstall *only* Numbux —————
        if (pkg in uninstallPackages && !BlockManager.isTemporarilyAllowed(pkg)) {
            // Inspect the on-screen hierarchy for your app’s name
            val root = rootInActiveWindow ?: return
            val isDialogForNumbux = root.findAccessibilityNodeInfosByText(appName).isNotEmpty()

            if (isDialogForNumbux) {
                // a) block touches when dialog first opens
                if (type == TYPE_WINDOW_STATE_CHANGED) {
                    blockAllTouchesFor(3_000)
                    return
                }
                // b) show PIN once content arrives
                if (type == TYPE_WINDOW_CONTENT_CHANGED || type == TYPE_WINDOWS_CHANGED) {
                    startActivity(Intent(this, PinActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra("app_package", pkg)
                    })
                    return
                }
            }
        }

        // ————— Now we know it’s not one of the SYSTEM flows —————

        // 4) Master toggle gates the rest
        if (!prefs.getBoolean("blocking_enabled", true)) return

        // 5) Skip system/UIs/your own
        if (pkg == "com.android.systemui"
            || pkg.startsWith("com.android.settings")
            || pkg.startsWith("com.samsung.android.spay")
            || pkg.startsWith("com.samsung.android.honeyboard")
            || pkg == applicationContext.packageName
        ) return

        // 6) Ignore launcher
        if (pkg == homePkg) return

        // 7) Reset per-app dismissals on app switch
        if (pkg != lastPackage) {
            if (BlockManager.isDismissed(pkg)) BlockManager.clearDismissed(pkg)
            BlockManager.resetAllDismissedIfPackageChanged(pkg)
        }
        lastPackage = pkg

        // 8) Never block your own PIN UI
        if (cls.contains("PinActivity", ignoreCase = true)) return

        // 9) Skip temporarily-allowed
        if (BlockManager.isTemporarilyAllowed(pkg)) return

        // ——— 10) Immediately lock the blocked app ———
        if (type == TYPE_WINDOW_STATE_CHANGED && BlockManager.isAppBlocked(pkg)) {
            Log.d("AppBlockerService", "⏱ Prompting PIN for $pkg immediately")
            // fire the PIN screen *right now*, no delay:
            startActivity(Intent(this, PinActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("app_package", pkg)
            })
            return
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