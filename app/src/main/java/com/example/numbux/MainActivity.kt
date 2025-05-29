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
import androidx.compose.ui.unit.dp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumbuXAppBar(
    drawerState: DrawerState,
    scope: CoroutineScope,
    enabled: Boolean
) {
    // derive tint from whether the drawer is open
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
                tint        = if (enabled) androidx.compose.ui.graphics.Color(0xFFFF6300) else androidx.compose.ui.graphics.Color.White,
                modifier    = Modifier.size(28.dp)
            )
        },
        actions = {
            Image(
                painter = painterResource(id = R.drawable.logo_blanco_numbux),
                contentDescription = "App logo",
                modifier = Modifier
                    .size(100.dp)           // adjust to fit your bar height
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
        private const val PREF_LAST_INTERNAL_CHANGE = "last_internal_wallpaper_change"
        private const val KEY_LAST_BACKUP_COLOR_HOME = "last_backup_primary_color_home"
        private const val KEY_LAST_BACKUP_COLOR_LOCK = "last_backup_primary_color_lock"
    }



    private lateinit var prefs: SharedPreferences
    private lateinit var blockingState: MutableState<Boolean>
    private var accessibilityDialog: AlertDialog? = null
    // Will hold the bitmap we override, so we can restore it later
    private var previousWallpaper: Bitmap? = null

    private var backupHomeUri: Uri?
        get() = prefs.getString("backup_home_uri", null)?.let(Uri::parse)
        set(v)   = prefs.edit().putString("backup_home_uri", v?.toString()).apply()

    private var backupLockUri: Uri?
        get() = prefs.getString("backup_lock_uri", null)?.let(Uri::parse)
        set(v)   = prefs.edit().putString("backup_lock_uri", v?.toString()).apply()

    private lateinit var showBackupHomePrompt: MutableState<Boolean>
    private lateinit var showBackupLockPrompt: MutableState<Boolean>

    private lateinit var dbRef: DatabaseReference
    private var wallpaperChangedReceiver: BroadcastReceiver? = null

    private lateinit var wallpaperColorsListener: WallpaperManager.OnColorsChangedListener
    private var lastInternalWallpaperChange: Long = 0L
    private var lastSeenColors: WallpaperColors? = null
    private var isInternalWallpaperChange = false

    @SuppressLint("NewApi")
    private fun checkForWallpaperChange() {
        if (isInternalWallpaperChange) return
        if (Build.VERSION.SDK_INT < VERSION_CODES.O_MR1) return
        val now = System.currentTimeMillis()
        if (now - lastInternalWallpaperChange < 2_000) return

        val wm = WallpaperManager.getInstance(this)

        // 1) fetch current & saved
        val currHome = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb() ?: -1
        val savedHome = prefs.getInt(KEY_LAST_BACKUP_COLOR_HOME, -1)
        val currLock = wm.getWallpaperColors(WallpaperManager.FLAG_LOCK)?.primaryColor?.toArgb() ?: -1
        val savedLock = prefs.getInt(KEY_LAST_BACKUP_COLOR_LOCK, -1)

        // 2) did either change?
        val homeChanged = savedHome != currHome
        val lockChanged = savedLock != currLock

        if (homeChanged || lockChanged) {
            // only show the buttons that actually changed
            showBackupHomePrompt.value = homeChanged
            showBackupLockPrompt.value = lockChanged

            // persist exactly those flags
            prefs.edit()
                .putBoolean("backup_home_prompt", homeChanged)
                .putBoolean("backup_lock_prompt", lockChanged)
                .apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val wallpaperManager: WallpaperManager =
            getSystemService(WallpaperManager::class.java)
        // hydrate Compose state from prefs
        showBackupHomePrompt = mutableStateOf(prefs.getBoolean("backup_home_prompt", false))
        showBackupLockPrompt = mutableStateOf(prefs.getBoolean("backup_lock_prompt", false))

        // Overlay permission
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                REQ_OVERLAY
            )
            return
        }

        // Accessibility service
        if (!isAccessibilityServiceEnabled(this)) {
            showEnableAccessibilityDialog()
        }

        // First-run initialization
        val hasInit = prefs.getBoolean("has_initialized", false)
        if (!hasInit) {
            // 1) disable blocking by default & mark initialized
            val editor = prefs.edit()
                .putBoolean("blocking_enabled", false)
                .putBoolean("has_initialized", true)

            // 2) snapshot current wallpapers as “last backup”
            val wm = WallpaperManager.getInstance(this)
            val homeColor = if (Build.VERSION.SDK_INT >= VERSION_CODES.O_MR1)
                wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                    ?.primaryColor?.toArgb() ?: -1
            else -1
            val lockColor = if (Build.VERSION.SDK_INT >= VERSION_CODES.O_MR1)
                wm.getWallpaperColors(WallpaperManager.FLAG_LOCK)
                    ?.primaryColor?.toArgb() ?: -1
            else -1
            editor.putInt(KEY_LAST_BACKUP_COLOR_HOME, homeColor)
                .putInt(KEY_LAST_BACKUP_COLOR_LOCK, lockColor)

            // 3) ask for both backups now
            editor.putBoolean("backup_home_prompt", true)
                .putBoolean("backup_lock_prompt", true)
                .apply()

            showBackupHomePrompt.value = true
            showBackupLockPrompt.value = true
        }

        // run a one-time catch-up in case the wallpaper changed while the app was dead
        checkForWallpaperChange()

        blockingState = mutableStateOf(prefs.getBoolean("blocking_enabled", false))

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

                // always apply whatever the DB says, skipping the PIN if remote
                runOnUiThread {
                    if (blockingState.value != remote) {
                        // 1) update the UI toggle
                        blockingState.value = remote
                        // 2) perform the same enable/disable flow (skipping remote writes)
                        val wm = WallpaperManager.getInstance(this@MainActivity)
                        if (remote)      enableBlocking(wm, writeRemote = false)
                        else             disableBlocking(wm, writeRemote = false)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("MainActivity", "Firebase listen failed", error.toException())
            }
        })

        // 7) Register a listener for any wallpaper change (internal or external)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val wm = WallpaperManager.getInstance(this)
            val handler = Handler(Looper.getMainLooper())

            wallpaperColorsListener = OnColorsChangedListener { colors, which ->
                if (isInternalWallpaperChange) return@OnColorsChangedListener
                val now = System.currentTimeMillis()

                // ignore any change we ourselves just did
                if (now - lastInternalWallpaperChange < 2_000) return@OnColorsChangedListener
                runOnUiThread {
                    when(which) {
                        WallpaperManager.FLAG_SYSTEM -> {
                            showBackupHomePrompt.value = true
                            showBackupLockPrompt.value = false
                            prefs.edit()
                                .putBoolean("backup_home_prompt", true)
                                .putBoolean("backup_lock_prompt", false)
                                .apply()
                        }
                        WallpaperManager.FLAG_LOCK -> {
                            showBackupHomePrompt.value = false
                            showBackupLockPrompt.value = true
                            prefs.edit()
                                .putBoolean("backup_home_prompt", false)
                                .putBoolean("backup_lock_prompt", true)
                                .apply()
                        }
                    }
                }
            }

            wm.addOnColorsChangedListener(wallpaperColorsListener, handler)
        }
        else {
            wallpaperChangedReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val now = System.currentTimeMillis()
                    if (now - lastInternalWallpaperChange < 2_000) return
                    runOnUiThread {

                        prefs.edit()
                            .putBoolean("backup_prompt_pending", true)
                            .apply()
                    }
                }
            }
            registerReceiver(
                wallpaperChangedReceiver,
                IntentFilter(Intent.ACTION_WALLPAPER_CHANGED)
            )
        }

        // 8) Now set up your Compose UI
        setContent {
            var currentPage by remember { mutableStateOf(1) }
            val enabled by blockingState
            val context = LocalContext.current

            // --------------- SAF picker launchers ----------------

            val pickWallpaperLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                uri?.also {
                    context.contentResolver.takePersistableUriPermission(
                        it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    // persist to prefs + internal file
                    val input = context.contentResolver.openInputStream(it)!!
                    File(context.filesDir, "wallpaper_backup.png")
                        .outputStream().use { dst -> input.copyTo(dst) }
                    Toast.makeText(context, "Fondo guardado en app.", Toast.LENGTH_SHORT).show()
                    prefs.edit().putBoolean("backup_prompt_pending", false).apply()
                    @SuppressLint("NewApi")
                    val primaryColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        WallpaperManager
                            .getInstance(context)
                            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                            ?.primaryColor
                            ?.toArgb() ?: -1
                    } else {
                        -1
                    }
                    prefs.edit().apply()
                }
            }

            val pickHomeLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                uri?.also {
                    context.contentResolver.takePersistableUriPermission(
                        it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    backupHomeUri = it
                    File(filesDir, "wallpaper_backup_home.png")
                        .outputStream().use { dst ->
                            context.contentResolver.openInputStream(it)!!.copyTo(dst)
                        }
                    showBackupHomePrompt.value = false
                    val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        WallpaperManager
                            .getInstance(context)
                            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                            ?.primaryColor
                            ?.toArgb() ?: -1
                    } else {
                        -1
                    }
                    prefs.edit()
                        .putBoolean("backup_home_prompt", false)
                        .putInt(KEY_LAST_BACKUP_COLOR_HOME, color)
                        .apply()
                    Toast.makeText(context, "Backup HOME guardado", Toast.LENGTH_SHORT).show()
                }
            }

            val pickLockLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                uri?.also {
                    context.contentResolver.takePersistableUriPermission(
                        it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    backupLockUri = it
                    File(filesDir, "wallpaper_backup_lock.png")
                        .outputStream().use { dst ->
                            context.contentResolver.openInputStream(it)!!.copyTo(dst)
                        }
                    showBackupLockPrompt.value = false
                    val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        WallpaperManager
                            .getInstance(context)
                            .getWallpaperColors(WallpaperManager.FLAG_LOCK)
                            ?.primaryColor
                            ?.toArgb() ?: -1
                    } else {
                        -1
                    }
                    prefs.edit()
                        .putBoolean("backup_lock_prompt", false)
                        .putInt(KEY_LAST_BACKUP_COLOR_LOCK, color)
                        .apply()
                    Toast.makeText(context, "Backup LOCK guardado", Toast.LENGTH_SHORT).show()
                }
            }

            // --------------- Drawer setup ----------------

            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier
                            .fillMaxHeight(0.9f)
                            .border(
                                width = 2.dp,
                                color = androidx.compose.ui.graphics.Color(0xCCFF6300),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(0.dp),
                        drawerShape          = RoundedCornerShape(16.dp),
                        drawerContainerColor = androidx.compose.ui.graphics.Color(0xB3000000),
                        drawerContentColor   = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "NumbuX",
                                color = androidx.compose.ui.graphics.Color(0xFFFF6300),
                                fontSize = 20.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))  // crea espacio extra bajo el texto

                            BlockerToggle(
                                enabled = blockingState.value,
                                onToggle = { wantsOff ->
                                    if (!wantsOff) {
                                        showDisablePinDialog { success ->
                                            if (success) {
                                                blockingState.value = false
                                                disableBlocking(wm = wallpaperManager)
                                            }
                                        }
                                    } else {
                                        blockingState.value = true
                                        enableBlocking(wm = wallpaperManager)
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
                                IconButton(
                                    onClick = {
                                        // go back to page 1 (or clamp at 1)
                                        currentPage = maxOf(1, currentPage - 1)
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
                                        // advance to page 2 (or whatever your max is)
                                        currentPage += 1
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
                                enabled     = blockingState.value
                            )
                        },
                        bottomBar = {
                            if (currentPage == 1) {
                                BasicCalculator()
                            } // else: nothing shows
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (showBackupHomePrompt.value) {
                                Button(onClick = { pickHomeLauncher.launch(arrayOf("image/*")) }) {
                                    Text("\uD83D\uDDBC Restaurar fondo HOME")
                                }
                            }

                            if (showBackupLockPrompt.value) {
                                Button(onClick = { pickLockLauncher.launch(arrayOf("image/*")) }) {
                                    Text("\uD83D\uDD12 Restaurar fondo BLOQUEO")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun enableBlocking(wm: WallpaperManager, writeRemote: Boolean = true) {
        // 1) Temporarily stop listening for changes (only on API 27+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wm.removeOnColorsChangedListener(wallpaperColorsListener)
        }

        // 2) record for your internal‐change guard
        lastInternalWallpaperChange = System.currentTimeMillis()

        // 3) load your custom wallpaper instead of a black screen
        val wallpaperBmp = BitmapFactory.decodeResource(
            resources,
            R.drawable.numbux_wallpaper_homelock
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // set for both home and lock
            wm.setBitmap(
                wallpaperBmp,
                /* visibleCrop= */ null,
                /* allowBackup= */ true,
                WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            )
        } else {
            wm.setBitmap(wallpaperBmp)
        }

        // 4) update your “last backup” colors & hide prompts
        //    (we assume your image has valid colors you want to treat as “backup”)
        val primary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                ?.primaryColor?.toArgb() ?: Color.BLACK
        } else Color.BLACK

        prefs.edit()
            .putInt(KEY_LAST_BACKUP_COLOR_HOME, primary)
            .putInt(KEY_LAST_BACKUP_COLOR_LOCK, primary)
            .putBoolean("backup_home_prompt", false)
            .putBoolean("backup_lock_prompt", false)
            .apply()
        showBackupHomePrompt.value = false
        showBackupLockPrompt.value = false

        // 5) Re‐attach the listener (only on API 27+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wm.addOnColorsChangedListener(wallpaperColorsListener, Handler(Looper.getMainLooper()))
        }

        // 6) finally write your blocking flag
        prefs.edit().putBoolean("blocking_enabled", true).apply()
        if (writeRemote) dbRef.setValue(true)
    }

    private fun disableBlocking(
        wm: WallpaperManager,
        writeRemote: Boolean = true
    ) {
        if (!writeRemote) {
            // remote‐off path: skip todo y restaurar silenciosamente
            isInternalWallpaperChange = true
            lastInternalWallpaperChange = System.currentTimeMillis()

            restoreHome(wm)
            restoreLock(wm)

            // leemos de nuevo los colores restaurados
            val newHome = if (Build.VERSION.SDK_INT >= VERSION_CODES.O_MR1)
                wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                    ?.primaryColor?.toArgb() ?: -1
            else -1
            val newLock = if (Build.VERSION.SDK_INT >= VERSION_CODES.O_MR1)
                wm.getWallpaperColors(WallpaperManager.FLAG_LOCK)
                    ?.primaryColor?.toArgb() ?: -1
            else -1

            prefs.edit()
                .putInt(KEY_LAST_BACKUP_COLOR_HOME, newHome)
                .putInt(KEY_LAST_BACKUP_COLOR_LOCK, newLock)
                .putBoolean("backup_home_prompt", false)
                .putBoolean("backup_lock_prompt", false)
                .putBoolean("blocking_enabled", false)
                .apply()

            showBackupHomePrompt.value = false
            showBackupLockPrompt.value = false

            isInternalWallpaperChange = false
            return
        }

        // off‐path normal: restaurar y luego escribir remoto, SIN pedir PIN
        isInternalWallpaperChange = true
        lastInternalWallpaperChange = System.currentTimeMillis()

        restoreHome(wm)
        restoreLock(wm)

        // leemos de nuevo los colores restaurados
        val newHome = if (Build.VERSION.SDK_INT >= VERSION_CODES.O_MR1)
            wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                ?.primaryColor?.toArgb() ?: -1
        else -1
        val newLock = if (Build.VERSION.SDK_INT >= VERSION_CODES.O_MR1)
            wm.getWallpaperColors(WallpaperManager.FLAG_LOCK)
                ?.primaryColor?.toArgb() ?: -1
        else -1

        prefs.edit()
            .putInt(KEY_LAST_BACKUP_COLOR_HOME, newHome)
            .putInt(KEY_LAST_BACKUP_COLOR_LOCK, newLock)
            .putBoolean("backup_home_prompt", false)
            .putBoolean("backup_lock_prompt", false)
            .putBoolean("blocking_enabled", false)
            .apply()

        showBackupHomePrompt.value = false
        showBackupLockPrompt.value = false

        isInternalWallpaperChange = false

        // sólo aquí empujamos el cambio a Firebase
        dbRef.setValue(false)
    }

    private fun handleBlockingToggle(isOn: Boolean) {
        blockingState.value = isOn
        val wm = WallpaperManager.getInstance(this)
        if (isOn) enableBlocking(wm)
        else disableBlocking(wm)
    }

    @SuppressLint("NewApi")
    private fun restoreWallpaper(wm: WallpaperManager) {
        // 1) Try the internal file
        val f = File(filesDir, "wallpaper_backup.png")
        if (f.exists()) {
            // Decode it in‐memory once
            val bmp = BitmapFactory.decodeFile(f.absolutePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wm.setBitmap(
                    bmp,
                    /* visibleCrop= */ null,
                    /* allowBackup= */ true,
                    WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                )
            } else {
                wm.setBitmap(bmp)
            }
            return
        }

        // 2) Fallback to the SAF URI if you really need it


        // 3) Nothing to load
        Toast.makeText(this, "No hay backup de fondo", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("NewApi")
    private fun restoreHome(wm: WallpaperManager) {
        val f = File(filesDir, "wallpaper_backup_home.png")
        if (f.exists()) {
            val bmp = BitmapFactory.decodeFile(f.absolutePath)
            wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM)
        } else if (backupHomeUri != null) {
            contentResolver.openInputStream(backupHomeUri!!)?.use {
                val bmp = BitmapFactory.decodeStream(it)
                wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM)
            }
        } else {
            Toast.makeText(this, "No hay backup HOME", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NewApi")
    private fun restoreLock(wm: WallpaperManager) {
        val f = File(filesDir, "wallpaper_backup_lock.png")
        if (f.exists()) {
            val bmp = BitmapFactory.decodeFile(f.absolutePath)
            wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
        } else if (backupLockUri != null) {
            contentResolver.openInputStream(backupLockUri!!)?.use {
                val bmp = BitmapFactory.decodeStream(it)
                wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
            }
        } else {
            Toast.makeText(this, "No hay backup LOCK", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        checkForWallpaperChange()

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

        val correctPin = prefs.getString("pin_app_lock", "1234")

        // Construir el diálogo
        val builder = AlertDialog.Builder(this)
            .setTitle("Ingrese PIN para desactivar")
            .setView(pinInput)
            .setPositiveButton("OK") { _, _ ->
                val entered = pinInput.text.toString()
                onResult(entered == correctPin)  // tu lógica de validación
            }
            .setNegativeButton("Cancelar") { _, _ ->
                onResult(false)
            }
            .setCancelable(false)

        // Mostrar el diálogo y capturar la instancia
        val dialog = builder.show()

        // Feedback de color al pulsar OK
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { btn ->
            btn.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> (btn as Button).setTextColor(Color.parseColor("#FF6300"))
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> (btn as Button).setTextColor(Color.WHITE)
                }
                false
            }
        }

        // Feedback de color al pulsar Cancelar
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { btn ->
            btn.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> (btn as Button).setTextColor(Color.parseColor("#FF6300"))
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> (btn as Button).setTextColor(Color.WHITE)
                }
                false
            }
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            WallpaperManager.getInstance(this)
                .removeOnColorsChangedListener(wallpaperColorsListener)
        } else {
            unregisterReceiver(wallpaperChangedReceiver)
        }
    }
}