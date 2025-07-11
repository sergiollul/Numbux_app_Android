package com.example.numbux

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.example.numbux.accessibility.AppBlockerService
import com.example.numbux.ui.theme.NumbuxTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import com.example.numbux.ui.BlockerToggle
import android.os.Build
import android.os.Build.VERSION_CODES
import com.google.firebase.database.DatabaseReference
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.app.WallpaperManager.OnColorsChangedListener
import android.os.Handler
import android.os.Looper
import android.app.WallpaperColors
import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import android.view.MotionEvent
import com.example.numbux.ui.BasicCalculator
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.setValue
import com.example.numbux.ui.DictionaryBottomBar
import com.example.numbux.ui.ScientificCalculator
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumbuXAppBar(
    drawerState: DrawerState,
    scope: CoroutineScope,
    enabled: Boolean
) {
    val iconTint by remember {
        derivedStateOf {
            if (drawerState.isOpen) androidx.compose.ui.graphics.Color(0xFFFF6300)
            else androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        }
    }

    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = {
                scope.launch {
                    if (drawerState.isClosed) drawerState.open()
                    else drawerState.close()
                } },
                modifier = Modifier
                    .padding(start = 18.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Menú",
                    tint = iconTint,
                    modifier = Modifier.size(38.dp)
                )
            }
        },
        title = {
            Icon(
                imageVector = if (enabled) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = "Bloqueo",
                tint = if (enabled) androidx.compose.ui.graphics.Color(0xFFFF6300) else androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(28.dp)
            )
        },
        actions = {
            Image(
                painter = painterResource(id = R.drawable.logo_blanco_numbux),
                contentDescription = "App logo",
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterVertically)
                    .padding(end = 6.dp)
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    companion object {
        private const val REQ_OVERLAY = 1001
        private const val KEY_LAST_BACKUP_COLOR_HOME = "last_backup_primary_color_home"
        private const val KEY_LAST_BACKUP_COLOR_LOCK = "last_backup_primary_color_lock"
        private const val PREF_SHOWN_BACKUP_EXPLANATION = "shown_backup_explanation"
    }

    private lateinit var prefs: SharedPreferences
    private val blockingState = mutableStateOf(false)
    private var accessibilityDialog: AlertDialog? = null
    private lateinit var showBackupHomePrompt: MutableState<Boolean>
    private lateinit var showBackupLockPrompt: MutableState<Boolean>
    private lateinit var dbRef: DatabaseReference
    private var wallpaperChangedReceiver: BroadcastReceiver? = null
    private lateinit var wallpaperColorsListener: WallpaperManager.OnColorsChangedListener
    private var isInternalWallpaperChange = false
    private var lastInternalWallpaperChange: Long = 0L
    private var hasShownBackupExplanation: Boolean
        get() = prefs.getBoolean(PREF_SHOWN_BACKUP_EXPLANATION, false)
        set(value) = prefs.edit().putBoolean(PREF_SHOWN_BACKUP_EXPLANATION, value).apply()
    private var showBackupDialog by mutableStateOf(false)
    private lateinit var accessibilityLauncher: ActivityResultLauncher<Intent>
    private lateinit var firebaseListener: ValueEventListener
    private lateinit var firebaseRef: DatabaseReference

    private var backupHomeUri: Uri?
        get() = prefs.getString("backup_home_uri", null)?.let(Uri::parse)
        set(v) = prefs.edit().putString("backup_home_uri", v?.toString()).apply()

    private var backupLockUri: Uri?
        get() = prefs.getString("backup_lock_uri", null)?.let(Uri::parse)
        set(v) = prefs.edit().putString("backup_lock_uri", v?.toString()).apply()

    @SuppressLint("NewApi")
    private fun checkForWallpaperChange() {
        if (isInternalWallpaperChange) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
        val now = System.currentTimeMillis()
        if (now - lastInternalWallpaperChange < 2_000) return

        val wm = WallpaperManager.getInstance(this)

        // Home wallpaper
        val currHome = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            ?.primaryColor
            ?.toArgb() ?: -1
        val savedHome = prefs.getInt(KEY_LAST_BACKUP_COLOR_HOME, -1)

        // Lock wallpaper
        val currLock = wm.getWallpaperColors(WallpaperManager.FLAG_LOCK)
            ?.primaryColor
            ?.toArgb() ?: -1
        val savedLock = prefs.getInt(KEY_LAST_BACKUP_COLOR_LOCK, -1)

        // Sólo mostrar “restore HOME” si Home cambió
        val homeChanged = currHome != savedHome

        // Sólo mostrar “restore LOCK” si Lock cambió y NO estamos en modo bloqueo
        val lockChanged = (currLock != savedLock) && !blockingState.value

        if (homeChanged || lockChanged) {
            showBackupHomePrompt.value = homeChanged
            showBackupLockPrompt.value = lockChanged

            prefs.edit()
                .putBoolean("backup_home_prompt", showBackupHomePrompt.value)
                .putBoolean("backup_lock_prompt", showBackupLockPrompt.value)
                .apply()
        }
    }

    private fun updateBlocking(enabled: Boolean, writeRemote: Boolean = true) {
        val wm = WallpaperManager.getInstance(this)
        if (enabled) {
            WallpaperHelper.enableLockWallpaper(this)
        } else {
            WallpaperHelper.restoreOriginalWallpapers(this)
        }
        blockingState.value = enabled

        // persist locally
        prefs.edit().putBoolean("blocking_enabled", enabled).apply()
        // optionally write remotely
        if (writeRemote) firebaseRef.setValue(enabled)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Accessibility launcher
        accessibilityLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { _ ->
            if (isAccessibilityServiceEnabled(this)) {
                accessibilityDialog?.dismiss()
                accessibilityDialog = null
            }
        }

        // 2) Preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // 3) Initialize your single blockingState
        blockingState.value = prefs.getBoolean("blocking_enabled", false)

        // 4) Backup prompts
        showBackupHomePrompt  = mutableStateOf(prefs.getBoolean("backup_home_prompt", false))
        showBackupLockPrompt  = mutableStateOf(prefs.getBoolean("backup_lock_prompt", false))

        // 5) Accessibility service check
        if (!isAccessibilityServiceEnabled(this)) {
            showEnableAccessibilityDialog()
        }

        // 6) First‐run initialization
        if (!prefs.getBoolean("has_initialized", false)) {
            val wm = WallpaperManager.getInstance(this)
            val homeColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
                wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb() ?: -1
            else -1
            val lockColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
                wm.getWallpaperColors(WallpaperManager.FLAG_LOCK)?.primaryColor?.toArgb() ?: -1
            else -1

            prefs.edit()
                .putBoolean("has_initialized", true)
                .putBoolean("blocking_enabled", false)
                .putInt(KEY_LAST_BACKUP_COLOR_HOME, homeColor)
                .putInt(KEY_LAST_BACKUP_COLOR_LOCK, lockColor)
                .putBoolean("backup_home_prompt", true)
                .putBoolean("backup_lock_prompt", true)
                .apply()

            showBackupHomePrompt.value = true
            showBackupLockPrompt.value = true
        }

        // 7) Catch‐up wallpaper state
        checkForWallpaperChange()

        // 8) Firebase listener setup
        val firebaseUrl = "https://numbux-790d6-default-rtdb.europe-west1.firebasedatabase.app"
        dbRef = Firebase.database(firebaseUrl)
            .getReference("rooms/testRoom/blocking_enabled")

        firebaseListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val remote = snapshot.getValue(Boolean::class.java) ?: false
                Log.i("MainActivity", "Remote toggled → $remote")
                runOnUiThread {
                    if (blockingState.value != remote) {
                        updateBlocking(remote, /* writeRemote = */ false)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("MainActivity", "Firebase listen failed", error.toException())
            }
        }

        // Wallpaper change listener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val wm = WallpaperManager.getInstance(this)
            wallpaperColorsListener = WallpaperManager.OnColorsChangedListener { colors, which ->
                if (!isInternalWallpaperChange && System.currentTimeMillis() - lastInternalWallpaperChange >= 2_000L) {
                    when (which) {
                        WallpaperManager.FLAG_SYSTEM -> {
                            showBackupHomePrompt.value = true
                        }
                        WallpaperManager.FLAG_LOCK -> {
                            // SOLO si el bloqueo NO está activo
                            if (!blockingState.value) {
                                showBackupLockPrompt.value = true
                            }
                        }
                    }
                    prefs.edit()
                        .putBoolean("backup_home_prompt", showBackupHomePrompt.value)
                        .putBoolean("backup_lock_prompt", showBackupLockPrompt.value)
                        .apply()
                }
            }
        } else {
            wallpaperChangedReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (System.currentTimeMillis() - lastInternalWallpaperChange >= 2_000L) {
                        prefs.edit().putBoolean("backup_prompt_pending", true).apply()
                    }
                }
            }
        }

        // Compose UI
        setContent {
            val context = LocalContext.current
            val wm = WallpaperManager.getInstance(context)

            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            var currentPage by remember { mutableStateOf(1) }
            val enabled by blockingState

            val pickHomeLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()) { uri -> uri?.let { saveBackup(it, "home") }}
            val pickLockLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()) { uri -> uri?.let { saveBackup(it, "lock") }}
            val maxPage = 3

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .fillMaxHeight(0.83f)
                            .border(
                                width = 2.dp,
                                color = androidx.compose.ui.graphics.Color(0xCCFF6300),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(0.dp),
                        drawerShape = RoundedCornerShape(16.dp),
                        drawerContainerColor = androidx.compose.ui.graphics.Color(0xB3000000),
                        drawerContentColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                        windowInsets = WindowInsets(0, 0, 0, 0),  // desactiva safe-area padding
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Spacer(modifier = Modifier.height(28.dp))  // crea espacio extra
                            Text(
                                text = "NumbuX",
                                color = androidx.compose.ui.graphics.Color(0xFFFF6300),
                                fontSize = 20.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))  // crea espacio extra

                            FocusModeToggle(
                                enabled = blockingState.value,
                                onToggle = { newState ->
                                    if (newState) {
                                        // no pin check on enable
                                        updateBlocking(true)
                                    } else {
                                        // show PIN dialog, then…
                                        showDisablePinDialog { ok ->
                                            if (ok) updateBlocking(false)
                                        }
                                    }
                                }
                            )

                            // ── Push everything above up ──────────────────────────────────────
                            Spacer(modifier = Modifier.weight(1f))


                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Texto de la herramienta actual
                                when (currentPage) {
                                    1 -> {
                                        Text(
                                            text = "Calculadora",
                                            fontSize = 16.sp,
                                            color = androidx.compose.ui.graphics.Color(0xFFFF6300),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                    2 -> {
                                        Text(
                                            text = "Científica",
                                            fontSize = 16.sp,
                                            color = androidx.compose.ui.graphics.Color(0xFFFF6300),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                    3 -> {
                                        Text(
                                            text = "Diccionario",
                                            fontSize = 16.sp,
                                            color = androidx.compose.ui.graphics.Color(0xFFFF6300),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                    else -> {
                                        // you can add more pages here
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        // only go back if we’re above page 1
                                        if (currentPage > 1) {
                                            currentPage--
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector        = Icons.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous page",
                                        modifier           = Modifier.size(46.dp),
                                        tint               = androidx.compose.ui.graphics.Color(0xFFFF6300)
                                    )
                                }
                                Text(
                                    text = "$currentPage",  // current page
                                    fontSize = 32.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                IconButton(
                                    onClick = {
                                        // only advance if we haven’t hit maxPage yet
                                        if (currentPage < maxPage) {
                                            currentPage++
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector        = Icons.Filled.KeyboardArrowRight,
                                        contentDescription = "Next page",
                                        modifier           = Modifier.size(46.dp),
                                        tint               = androidx.compose.ui.graphics.Color(0xFFFF6300)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cambiar Herramienta",
                                    fontSize = 10.sp,
                                )
                            }
                            // … más items
                        }
                    }
                }
            ) {
                NumbuxTheme {
                    Scaffold(
                        topBar = {
                            NumbuXAppBar(
                                drawerState = drawerState,
                                scope       = scope,
                                enabled     = enabled
                            )
                        },
                        // Only calculators live here now
                        bottomBar = {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                            ) {
                                when (currentPage) {
                                    1 -> BasicCalculator()
                                    2 -> ScientificCalculator()
                                    3 -> {
                                        // only for the dictionary, add 16.dp above it
                                        Box(modifier = Modifier.padding(top = 20.dp)) {
                                            DictionaryBottomBar()
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Your “Restaurar” buttons
                            if (!blockingState.value) {
                                     if (showBackupHomePrompt.value) {
                                             Button({ pickHomeLauncher.launch(arrayOf("image/*")) }) {
                                                     Text("🖼 Restaurar HOME")
                                                 }
                                         }
                                     if (showBackupLockPrompt.value) {
                                             Button({ pickLockLauncher.launch(arrayOf("image/*")) }) {
                                                     Text("🔒 Restaurar LOCK")
                                                 }
                                         }
                                 }

                            // Now render page 3’s dictionary in the content,
                            // with its spacer to push it away from the topBar
                            when (currentPage) {
                                3 -> {
                                    Spacer(Modifier.height(16.dp))    // <-- breathing room under the topBar
                                    DictionaryBottomBar()
                                }
                            }
                        }

                        if (showBackupDialog) {
                            BackupExplanationDialog { showBackupDialog = false }
                        }
                    }
                }
            }
        }

        // Overlay permission
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                REQ_OVERLAY
            )
            return
        }
    }

    /**
     * Helper to save backup images
     */
    private fun saveBackup(uri: Uri, type: String) {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val fileName = if(type=="home") "wallpaper_backup_home.png" else "wallpaper_backup_lock.png"
        File(filesDir, fileName).outputStream().use { contentResolver.openInputStream(uri)!!.copyTo(it) }
        val keyPrompt = if(type=="home") "backup_home_prompt" else "backup_lock_prompt"
        val keyColor = if(type=="home") KEY_LAST_BACKUP_COLOR_HOME else KEY_LAST_BACKUP_COLOR_LOCK
        val flag = if(type=="home") showBackupHomePrompt else showBackupLockPrompt
        flag.value = false
        val color = if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O_MR1) WallpaperManager.getInstance(this)
            .getWallpaperColors(if(type=="home") WallpaperManager.FLAG_SYSTEM else WallpaperManager.FLAG_LOCK)
            ?.primaryColor?.toArgb() ?: -1 else -1
        prefs.edit().putBoolean(keyPrompt,false).putInt(keyColor,color).apply()
        Toast.makeText(this,"Fondo Guardado",Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        checkForWallpaperChange()

        if (!isAccessibilityServiceEnabled(this)) {
            // service still off → show it if not showing
            if (accessibilityDialog?.isShowing != true) {
                showEnableAccessibilityDialog()
            }
        } else {
            // service now on → dismiss it immediately
            accessibilityDialog?.dismiss()
            accessibilityDialog = null

            // Solo si aún **no** lo hemos mostrado y el servicio YA está ON
            if (!hasShownBackupExplanation) {
                showBackupExplanation()
                hasShownBackupExplanation = true   // se persiste en prefs
            }
        }
    }

    private fun showBackupExplanation() {
        showBackupDialog = true
    }

    @Composable
    fun BackupExplanationDialog(onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),

            // ← only one modifier block, combining width, background & padding
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(
                    width = 2.dp,
                    color = androidx.compose.ui.graphics.Color(0xFFFF6300),
                    shape = RoundedCornerShape(16.dp)
                ),
            containerColor = androidx.compose.ui.graphics.Color(0xFF000000),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(16.dp),

            // Title if you need it:
            title = {
                Text(
                    text = "Restaurar Fondos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White
                )
            },

            text = {
                Column {
                    Text(
                        text = "¿Qué son estos botones?",
                        style = MaterialTheme.typography.titleMedium,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = { /* no-op */ },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFFF6300),
                            disabledContainerColor = androidx.compose.ui.graphics.Color(0xFFFF6300),
                            contentColor = androidx.compose.ui.graphics.Color.White,
                            disabledContentColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) {
                        Text(
                            text = "🖼 Restaurar HOME",
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = { /* no-op */ },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFFF6300),
                            disabledContainerColor = androidx.compose.ui.graphics.Color(0xFFFF6300),
                            contentColor = androidx.compose.ui.graphics.Color.White,
                            disabledContentColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) {
                        Text(
                            text = "🔒 Restaurar LOCK",
                            style = MaterialTheme.typography.labelSmall,
                            color = androidx.compose.ui.graphics.Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = """
                        Cuando el modo foco esté activado, tu fondo de pantalla y de bloqueo cambiarán.
                        
                        Estos botones son para que NumbuX restaure tus fondos una vez el modo foco se desactive.
                        
                        Por favor, selecciona tus fondos con estos botones.
                    """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            },

            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
    }

    @Composable
    fun FocusModeToggle(
        enabled: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Modo Foco:",
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color.White
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        // ON
                        checkedThumbColor  = androidx.compose.ui.graphics.Color.Black,
                        checkedBorderColor = androidx.compose.ui.graphics.Color.Black,
                        checkedTrackColor  = androidx.compose.ui.graphics.Color(0xFFFF6300),
                        // OFF
                        uncheckedThumbColor  = androidx.compose.ui.graphics.Color(0xFFFF6300),
                        uncheckedBorderColor = androidx.compose.ui.graphics.Color(0xFFFF6300),
                        uncheckedTrackColor  = androidx.compose.ui.graphics.Color.Black
                    )
                )
                Text(
                    text = if (enabled) "Activado" else "Desactivado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context):Boolean{
        val am=context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val component=ComponentName(context,AppBlockerService::class.java)
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
            .any{it.resolveInfo.serviceInfo.run{packageName==component.packageName && name==component.className}}
    }

    private fun showEnableAccessibilityDialog(){
        val custom=layoutInflater.inflate(R.layout.dialog_enable_accessibility,null,false)
        val dlg=AlertDialog.Builder(this,R.style.NoShadowDialog).setView(custom).setCancelable(false).show()
        accessibilityDialog = dlg
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        custom.viewTreeObserver.addOnGlobalLayoutListener(object:ViewTreeObserver.OnGlobalLayoutListener{
            override fun onGlobalLayout(){ custom.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dialogHeightPx=custom.height
                val btn = Button(this@MainActivity).apply {
                    text = "Abrir Ajustes"
                    background = ContextCompat.getDrawable(context, R.drawable.button_bg_pressed_selector)
                    setTextColor(Color.BLACK)
                    val pad = (16 * resources.displayMetrics.density).toInt()
                    setPadding(pad, pad / 2, pad, pad / 2)
                    setOnClickListener {
                        accessibilityLauncher.launch(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        )
                        dlg.dismiss()
                        accessibilityDialog = null
                    }
                    setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> setTextColor(Color.parseColor("#FF6300"))
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> setTextColor(Color.BLACK)
                        }
                        false
                    }
                }
                dlg.window
                    ?.decorView
                    ?.findViewById<FrameLayout>(android.R.id.content)
                    ?.let { container ->
                        val params = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
                            topMargin = dialogHeightPx + (8 * resources.displayMetrics.density).toInt()
                        }
                        container.addView(btn, params)
                    }
            }
        })
    }

    private fun showDisablePinDialog(onResult:(Boolean)->Unit){
        val pinInput=EditText(this).apply{ inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD; hint="####" }
        val correctPin=prefs.getString("pin_app_lock","1234")
        val dialog=AlertDialog.Builder(this).setTitle("Ingrese PIN para desactivar").setView(pinInput).setPositiveButton("OK"){_,_->onResult(pinInput.text.toString()==correctPin)}.setNegativeButton("Cancelar"){_,_->onResult(false)}.setCancelable(false).show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnTouchListener{v,e-> if(e.action==MotionEvent.ACTION_DOWN)(v as Button).setTextColor(Color.parseColor("#FF6300")) else(v as Button).setTextColor(Color.WHITE); false }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setOnTouchListener{v,e-> if(e.action==MotionEvent.ACTION_DOWN)(v as Button).setTextColor(Color.parseColor("#FF6300")) else(v as Button).setTextColor(Color.WHITE); false }
    }

    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==REQ_OVERLAY){ if(Settings.canDrawOverlays(this)) recreate() else{ Toast.makeText(this,"Se necesita permiso para mostrar sobre otras apps",Toast.LENGTH_LONG).show(); finish() }}
    }

    override fun onStart() {
        super.onStart()
        // Firebase
        dbRef.addValueEventListener(firebaseListener)

        // Wallpaper‐colors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            WallpaperManager
                .getInstance(this)
                .addOnColorsChangedListener(
                    wallpaperColorsListener,
                    Handler(Looper.getMainLooper())
                )
        } else {
            wallpaperChangedReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    // …
                }
            }
            registerReceiver(
                wallpaperChangedReceiver,
                IntentFilter(Intent.ACTION_WALLPAPER_CHANGED)
            )
        }
    }

    override fun onStop() {
        super.onStop()
        // Firebase
        dbRef.removeEventListener(firebaseListener)

        // Wallpaper‐colors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            WallpaperManager
                .getInstance(this)
                .removeOnColorsChangedListener(wallpaperColorsListener)
        } else {
            wallpaperChangedReceiver?.let { unregisterReceiver(it) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}