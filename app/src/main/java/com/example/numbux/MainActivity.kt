package com.example.numbux

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.example.numbux.accessibility.AppBlockerService
import com.example.numbux.control.BlockManager
import com.example.numbux.ui.PinActivity
import com.example.numbux.ui.theme.NumbuxTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.core.content.ContextCompat
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.graphics.Color
import android.view.ViewTreeObserver
import android.graphics.drawable.ColorDrawable
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.platform.LocalContext
import com.example.numbux.ui.BlockerToggle
import android.os.Build
import android.os.Build.VERSION_CODES
import com.example.numbux.ui.BackupWallpaperButton
import com.google.firebase.database.DatabaseReference
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.compose.rememberLauncherForActivityResult


@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    companion object {
        private const val REQ_OVERLAY = 1001
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var blockingState: MutableState<Boolean>
    private lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener
    private var accessibilityDialog: AlertDialog? = null
    // Will hold the bitmap we override, so we can restore it later
    private var previousWallpaper: Bitmap? = null
    private var backupWallpaperUri: Uri?
        get() = prefs.getString("backup_wallpaper_uri", null)?.let(Uri::parse)
        set(value) = prefs.edit().putString("backup_wallpaper_uri", value?.toString()).apply()

    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Overlay‐permission check (unchanged)…
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                REQ_OVERLAY
            )
            return
        }

        // 2) Accessibility-service check
        if (!isAccessibilityServiceEnabled(this)) {
            showEnableAccessibilityDialog()
        }

        // 3) Initialize SharedPreferences & Compose state
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // ————— First-run initialization —————
        if (!prefs.getBoolean("has_initialized", false)) {
            // On a brand-new install, force the toggle OFF:
            prefs.edit()
                .putBoolean("blocking_enabled", false)
                .putBoolean("has_initialized", true)
                .apply()
        }

        blockingState = mutableStateOf(prefs.getBoolean("blocking_enabled", false))

        // 4) Listen for external prefs changes
        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == "blocking_enabled") {
                // update our Compose state
                blockingState.value = sp.getBoolean(key, false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        // 5) Firebase remote listener (unchanged)
        val room = "testRoom"
        val firebaseUrl = "https://numbux-790d6-default-rtdb.europe-west1.firebasedatabase.app"
        dbRef = Firebase.database(firebaseUrl)
            .getReference("rooms")
            .child(room)
            .child("blocking_enabled")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val remote = snapshot.getValue(Boolean::class.java) ?: false
                // write to prefs *and* state – triggers our prefListener
                prefs.edit().putBoolean("blocking_enabled", remote).apply()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("MainActivity", "Firebase listen failed", error.toException())
            }
        })

        // 6) Now set up your Compose UI
        setContent {
            val enabled by blockingState
            val context = LocalContext.current

            // 1) Create an OpenDocument launcher
            val pickWallpaperLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
                onResult = { uri: Uri? ->
                    uri?.let {
                        // 1) Toma permiso persistente para leer este URI siempre
                        context.contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )

                        // 2) Guárdalo en prefs
                        backupWallpaperUri = it
                        Toast.makeText(context, "Fondo guardado.", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            NumbuxTheme {
                Scaffold(topBar = { TopAppBar(title = { Text("NumbuX") }) }) { inner ->
                    Column(
                        Modifier
                                .fillMaxSize()
                                .padding(inner)
                                .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                        Text(
                            "Bienvenido a NumbuX",
                            style = MaterialTheme.typography.headlineSmall
                                    )

                        // 5) **Show the button only if we haven't already backed up**:
                        if (backupWallpaperUri == null) {
                            Button(
                                onClick = {
                                    // Launch the SAF picker when clicked
                                    pickWallpaperLauncher.launch(arrayOf("image/*"))
                                }
                            ) {
                                Text("Selecciona tu fondo para restaurar")
                            }
                        }

                        // ← call your new composable here:
                        BlockerToggle(
                            enabled = enabled,
                            onToggle = ::handleBlockingToggle
                                    )

                        // ← keep the status text if you like
                        Text(
                            if (enabled) "El bloqueador está ACTIVADO"
                                    else "El bloqueador está DESACTIVADO",
                            style = MaterialTheme.typography.bodyMedium
                                    )
                        }
                    }
                }
        }
    }

    private fun handleBlockingToggle(isOn: Boolean, wm: WallpaperManager) {
        if (isOn) {
            // 1) Create & set a pure-black wallpaper
            val w = wm.desiredMinimumWidth
            val h = wm.desiredMinimumHeight
            val blackBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                .apply { eraseColor(android.graphics.Color.BLACK) }
            wm.setBitmap(blackBmp)

            // 2) Mark as enabled in prefs & Firebase
            prefs.edit().putBoolean("blocking_enabled", true).apply()
            dbRef.setValue(true)

        } else {
            showDisablePinDialog { success ->
                if (!success) return@showDisablePinDialog

                // 1) restaurar condicionalmente
                val uri = backupWallpaperUri
                if (uri == null) {
                    Toast.makeText(
                        this,
                        "No tienes un fondo guardado para restaurar",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    try {
                        contentResolver.openInputStream(uri)?.use { stream ->
                            wm.setStream(stream)
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error al restaurar el fondo", e)
                        Toast.makeText(this, "Error al restaurar el fondo", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                // 2) marcar OFF
                prefs.edit().putBoolean("blocking_enabled", false).apply()
                dbRef.setValue(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!isAccessibilityServiceEnabled(this)) {
            if (accessibilityDialog?.isShowing != true) {
                showEnableAccessibilityDialog()
            }
        } else {
            accessibilityDialog?.dismiss()
            accessibilityDialog = null
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val component = ComponentName(context, AppBlockerService::class.java)
        val enabledServices = am
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        return enabledServices.any {
            val info = it.resolveInfo.serviceInfo
            info.packageName == component.packageName &&
                    info.name == component.className
        }
    }


    private fun showEnableAccessibilityDialog() {
        // 1) Inflate your custom content (title + message)
        val custom = layoutInflater.inflate(
            R.layout.dialog_enable_accessibility,
            null,
            false
        )

        // 2) Build & show *with* our no-shadow style
        val dlg = AlertDialog.Builder(this, R.style.NoShadowDialog)
            .setView(custom)
            .setCancelable(false)
            .show()

        // 3) Force the window’s background to fully transparent
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 4) Wait until our custom view is measured, so we know its height
        custom.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                custom.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // height of the dialog’s content
                val dialogHeightPx = custom.height

                // 5) Create the “Abrir Ajustes” button
                val btn = Button(this@MainActivity).apply {
                    text = "Abrir Ajustes"
                    // use our new selectors
                    background = ContextCompat.getDrawable(context, R.drawable.button_bg_pressed_selector)
                    setTextColor(ContextCompat.getColorStateList(context, R.color.button_text_pressed_selector))
                    val pad = (16 * resources.displayMetrics.density).toInt()
                    setPadding(pad, pad/2, pad, pad/2)
                    setOnClickListener {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        dlg.dismiss()
                    }
                }

                // 6) Inject the button just *below* the dialog content
                dlg.window
                    ?.decorView
                    ?.findViewById<FrameLayout>(android.R.id.content)
                    ?.let { container ->
                        val topMarginPx = dialogHeightPx + (8 * resources.displayMetrics.density).toInt()
                        val params = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
                            this.topMargin = topMarginPx
                        }
                        container.addView(btn, params)
                    }
            }
        })
    }

    private fun showDisablePinDialog(onResult: (Boolean) -> Unit) {
        val pinInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "####"
        }
        AlertDialog.Builder(this)
            .setTitle("Ingrese PIN para desactivar")
            .setView(pinInput)
            .setPositiveButton("OK") { _, _ ->
                val entered = pinInput.text.toString()
                val correct = prefs.getString("pin_app_lock", "1234")
                if (entered == correct) onResult(true)
                else {
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            }
            .setNegativeButton("Cancelar") { _, _ -> onResult(false) }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) recreate()
            else {
                Toast.makeText(this,
                    "Se necesita permiso para mostrar sobre otras apps", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}