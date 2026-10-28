package com.jondude1.drivedeck

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.jondude1.drivedeck.data.AppEntry
import com.jondude1.drivedeck.data.AppRepository
import com.jondude1.drivedeck.data.Prefs
import com.jondude1.drivedeck.ui.DriveDeckTheme
import com.jondude1.drivedeck.ui.HomeScreen
import com.jondude1.drivedeck.update.UpdateInfo
import com.jondude1.drivedeck.update.UpdateState
import com.jondude1.drivedeck.update.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private val apps = mutableStateOf<List<AppEntry>>(emptyList())
    private val updateState = mutableStateOf<UpdateState>(UpdateState.Idle)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        setContent {
            DriveDeckTheme {
                LaunchedEffect(prefs.keepScreenOn, prefs.immersive) { applyWindowSettings() }

                HomeScreen(
                    apps = apps.value,
                    prefs = prefs,
                    updateState = updateState.value,
                    onLaunch = { entry ->
                        if (!AppRepository.launch(this@MainActivity, entry)) {
                            Toast.makeText(
                                this@MainActivity,
                                "Can't open ${entry.label}",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    onAppInfo = { entry -> AppRepository.openAppInfo(this@MainActivity, entry) },
                    onUpdateNow = {
                        val state = updateState.value
                        if (state is UpdateState.Available) startUpdate(state.info)
                    },
                    onDismissUpdate = { updateState.value = UpdateState.Idle },
                    onCheckUpdate = { checkForUpdate(manual = true) },
                    onOpenHomeSettings = { openHomeSettings() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshApps()
        checkForUpdate(manual = false)
    }

    private fun refreshApps() {
        lifecycleScope.launch {
            val list = withContext(Dispatchers.Default) { AppRepository.load(applicationContext) }
            apps.value = list
            prefs.ensureDefaultPins(list)
        }
    }

    private fun applyWindowSettings() {
        if (prefs.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (prefs.immersive) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun openHomeSettings() {
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    // ------------------------------------------------------------ self-update

    private fun checkForUpdate(manual: Boolean) {
        val current = updateState.value
        if (current is UpdateState.Downloading || current is UpdateState.Checking) return
        if (!manual && System.currentTimeMillis() - prefs.lastUpdateCheck < CHECK_INTERVAL_MS) return
        if (manual) updateState.value = UpdateState.Checking

        lifecycleScope.launch {
            prefs.lastUpdateCheck = System.currentTimeMillis()
            try {
                val info = withContext(Dispatchers.IO) { Updater.check(BuildConfig.VERSION_CODE) }
                updateState.value = when {
                    info != null -> UpdateState.Available(info)
                    manual -> UpdateState.UpToDate
                    else -> UpdateState.Idle
                }
            } catch (e: Exception) {
                updateState.value = if (manual) {
                    UpdateState.Failed("Couldn't check for updates: ${e.message ?: "no connection"}")
                } else {
                    UpdateState.Idle
                }
            }
        }
    }

    private fun startUpdate(info: UpdateInfo) {
        if (!Updater.ensureInstallPermission(this)) {
            updateState.value = UpdateState.Failed(
                "Allow 'Install unknown apps' for DriveDeck, then tap Update again."
            )
            return
        }
        lifecycleScope.launch {
            updateState.value = UpdateState.Downloading(info, 0)
            try {
                val apk = withContext(Dispatchers.IO) {
                    Updater.download(applicationContext, info) { percent ->
                        updateState.value = UpdateState.Downloading(info, percent)
                    }
                }
                Updater.install(this@MainActivity, apk)
                updateState.value = UpdateState.Idle
            } catch (e: Exception) {
                updateState.value = UpdateState.Failed("Update failed: ${e.message ?: "unknown error"}")
            }
        }
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 30 * 60 * 1000L
    }
}
