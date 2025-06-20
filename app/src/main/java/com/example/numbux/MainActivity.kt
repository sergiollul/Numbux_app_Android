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
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var blockingState: MutableState<Boolean>
    private var accessibilityDialog: AlertDialog? = null
    private lateinit var showBackupHomePrompt: MutableState<Boolean>
    private lateinit var showBackupLockPrompt: MutableState<Boolean>
    private lateinit var dbRef: DatabaseReference
    private var wallpaperChangedReceiver: BroadcastReceiver? = null
    private lateinit var wallpaperColorsListener: WallpaperManager.OnColorsChangedListener
    private var isInternalWallpaperChange = false
    private var lastInternalWallpaperChange: Long = 0L

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
        val currHome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb() ?: -1
        else -1
        val savedHome = prefs.getInt(KEY_LAST_BACKUP_COLOR_HOME, -1)

        val currLock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            wm.getWallpaperColors(WallpaperManager.FLAG_LOCK)?.primaryColor?.toArgb() ?: -1
        else -1
        val savedLock = prefs.getInt(KEY_LAST_BACKUP_COLOR_LOCK, -1)

        if (savedHome != currHome || savedLock != currLock) {
            showBackupHomePrompt.value = savedHome != currHome
            showBackupLockPrompt.value = savedLock != currLock
            prefs.edit()
                .putBoolean("backup_home_prompt", showBackupHomePrompt.value)
                .putBoolean("backup_lock_prompt", showBackupLockPrompt.value)
                .apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
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
        if (!isAccessibilityServiceEnabled(this)) showEnableAccessibilityDialog()

        // First-run init
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

        // Catch-up wallpaper
        checkForWallpaperChange()

        blockingState = mutableStateOf(prefs.getBoolean("blocking_enabled", false))

        // Firebase listener
        val firebaseUrl = "https://numbux-790d6-default-rtdb.europe-west1.firebasedatabase.app"
        dbRef = Firebase.database(firebaseUrl)
            .getReference("rooms/testRoom/blocking_enabled")
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val remote = snapshot.getValue(Boolean::class.java) ?: false
                runOnUiThread {
                    if (blockingState.value != remote) {
                        blockingState.value = remote
                        val wm = WallpaperManager.getInstance(this@MainActivity)
                        if (remote) enableBlocking(wm, false) else disableBlocking(wm, false)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("MainActivity", "Firebase listen failed", error.toException())
            }
        })

        // Wallpaper change listener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val wm = WallpaperManager.getInstance(this)
            wallpaperColorsListener = WallpaperManager.OnColorsChangedListener { colors, which ->
                if (!isInternalWallpaperChange && System.currentTimeMillis() - lastInternalWallpaperChange >= 2_000L) {
                    when(which) {
                        WallpaperManager.FLAG_SYSTEM -> showBackupHomePrompt.value = true
                        WallpaperManager.FLAG_LOCK -> showBackupLockPrompt.value = true
                    }
                    prefs.edit()
                        .putBoolean("backup_home_prompt", showBackupHomePrompt.value)
                        .putBoolean("backup_lock_prompt", showBackupLockPrompt.value)
                        .apply()
                }
            }
            wm.addOnColorsChangedListener(wallpaperColorsListener, Handler(Looper.getMainLooper()))
        } else {
            wallpaperChangedReceiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (System.currentTimeMillis() - lastInternalWallpaperChange >= 2_000L) {
                        prefs.edit().putBoolean("backup_prompt_pending", true).apply()
                    }
                }
            }
            registerReceiver(wallpaperChangedReceiver, IntentFilter(Intent.ACTION_WALLPAPER_CHANGED))
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
                        drawerShape = RoundedCornerShape(16.dp),
                        drawerContainerColor = androidx.compose.ui.graphics.Color(0xB3000000),
                        drawerContentColor = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
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
                                                disableBlocking(wm)
                                            }
                                        }
                                    } else {
                                        blockingState.value = true
                                        enableBlocking(wm)
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
                            // Pass the same drawerState & scope here
                            NumbuXAppBar(
                                drawerState = drawerState,
                                scope       = scope,
                                enabled     = enabled
                            )
                        },
                        bottomBar = {
                            when (currentPage) {
                                1 -> BasicCalculator()
                                2 -> ScientificCalculator()
                                3 -> DictionaryBottomBar()
                            }
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
                            if (showBackupHomePrompt.value) Button({ pickHomeLauncher.launch(arrayOf("image/*")) }) { Text("🖼 Restaurar HOME") }
                            if (showBackupLockPrompt.value) Button({ pickLockLauncher.launch(arrayOf("image/*")) }) { Text("🔒 Restaurar LOCK") }
                        }
                    }
                }
            }
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

    private fun doWallpaperSwap(action: ()->Unit) {
        isInternalWallpaperChange=true
        lastInternalWallpaperChange=System.currentTimeMillis()
        action()
        Handler(Looper.getMainLooper()).postDelayed({isInternalWallpaperChange=false},300)
    }

    private fun enableBlocking(wm: WallpaperManager, writeRemote:Boolean=true) {
        val bmp=BitmapFactory.decodeResource(resources,R.drawable.numbux_wallpaper_homelock)
        doWallpaperSwap{ if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N) wm.setBitmap(bmp,null,true,WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) else wm.setBitmap(bmp) }
        val primary= if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O_MR1) wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb()?:Color.BLACK else Color.BLACK
        prefs.edit()
            .putInt(KEY_LAST_BACKUP_COLOR_HOME,primary)
            .putInt(KEY_LAST_BACKUP_COLOR_LOCK,primary)
            .putBoolean("backup_home_prompt",false)
            .putBoolean("backup_lock_prompt",false)
            .putBoolean("blocking_enabled",true)
            .apply()
        showBackupHomePrompt.value=false; showBackupLockPrompt.value=false
        if(writeRemote) dbRef.setValue(true)
        blockingState.value=true
    }

    private fun disableBlocking(wm: WallpaperManager, writeRemote: Boolean = true) {
        // Suppress listener and restore both home and lock wallpapers
        doWallpaperSwap {
            restoreHome(wm)
            restoreLock(wm)
        }

        // Re-read and persist the restored colors
        val newHome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                ?.primaryColor
                ?.toArgb() ?: -1
        } else -1

        val newLock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            wm.getWallpaperColors(WallpaperManager.FLAG_LOCK)
                ?.primaryColor
                ?.toArgb() ?: -1
        } else -1

        prefs.edit()
            .putInt(KEY_LAST_BACKUP_COLOR_HOME, newHome)
            .putInt(KEY_LAST_BACKUP_COLOR_LOCK, newLock)
            .putBoolean("backup_home_prompt", false)
            .putBoolean("backup_lock_prompt", false)
            .putBoolean("blocking_enabled", false)
            .apply()

        showBackupHomePrompt.value = false
        showBackupLockPrompt.value = false

        // Write remote flag if needed
        if (writeRemote) dbRef.setValue(false)
        blockingState.value = false
    }

    @SuppressLint("NewApi")
    private fun restoreWallpaper(wm: WallpaperManager) {
        val f=File(filesDir,"wallpaper_backup.png")
        if(f.exists()){ val bmp=BitmapFactory.decodeFile(f.absolutePath)
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N) wm.setBitmap(bmp,null,true,WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) else wm.setBitmap(bmp)
        } else Toast.makeText(this,"No hay backup de fondo",Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("NewApi")
    private fun restoreHome(wm: WallpaperManager) {
        val f=File(filesDir,"wallpaper_backup_home.png")
        when {
            f.exists()->{ val bmp=BitmapFactory.decodeFile(f.absolutePath); wm.setBitmap(bmp,null,true,WallpaperManager.FLAG_SYSTEM) }
            backupHomeUri!=null-> contentResolver.openInputStream(backupHomeUri!!)?.use{val bmp=BitmapFactory.decodeStream(it); wm.setBitmap(bmp,null,true,WallpaperManager.FLAG_SYSTEM)}
            else->Toast.makeText(this,"No hay backup HOME",Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NewApi")
    private fun restoreLock(wm: WallpaperManager) {
        val f=File(filesDir,"wallpaper_backup_lock.png")
        when {
            f.exists()->{val bmp=BitmapFactory.decodeFile(f.absolutePath);wm.setBitmap(bmp,null,true,WallpaperManager.FLAG_LOCK)}
            backupLockUri!=null-> contentResolver.openInputStream(backupLockUri!!)?.use{val bmp=BitmapFactory.decodeStream(it); wm.setBitmap(bmp,null,true,WallpaperManager.FLAG_LOCK)}
            else->Toast.makeText(this,"No hay backup LOCK",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume(); checkForWallpaperChange(); if(!isAccessibilityServiceEnabled(this) && accessibilityDialog?.isShowing!=true) showEnableAccessibilityDialog() else accessibilityDialog?.dismiss()
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
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        dlg.dismiss()
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

    override fun onDestroy(){
        super.onDestroy()
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O_MR1) WallpaperManager.getInstance(this).removeOnColorsChangedListener(wallpaperColorsListener)
        else unregisterReceiver(wallpaperChangedReceiver)
    }
}


