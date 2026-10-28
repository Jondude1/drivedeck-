@file:OptIn(ExperimentalFoundationApi::class)

package com.jondude1.drivedeck.ui

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jondude1.drivedeck.BuildConfig
import com.jondude1.drivedeck.data.AppEntry
import com.jondude1.drivedeck.data.IconCache
import com.jondude1.drivedeck.data.Prefs
import com.jondude1.drivedeck.update.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class TileSpec(val tile: Dp, val icon: Dp, val label: TextUnit)

private fun tileSpec(extraLarge: Boolean): TileSpec =
    if (extraLarge) TileSpec(176.dp, 88.dp, 20.sp) else TileSpec(136.dp, 64.dp, 16.sp)

@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    prefs: Prefs,
    updateState: UpdateState,
    onLaunch: (AppEntry) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onUpdateNow: () -> Unit,
    onDismissUpdate: () -> Unit,
    onCheckUpdate: () -> Unit,
    onOpenHomeSettings: () -> Unit,
) {
    val spec = tileSpec(prefs.extraLarge)
    val visible = remember(apps, prefs.hidden) { apps.filter { it.key !in prefs.hidden } }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    val pinnedApps = prefs.pinned.mapNotNull { byKey[it] }.filter { it.key !in prefs.hidden }

    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        UpdateBanner(updateState, onUpdateNow, onDismissUpdate)
        TopBar(onSettings = { showSettings = true })
        Spacer(Modifier.height(8.dp))

        SectionLabel("PINNED")
        PinnedRow(
            apps = pinnedApps,
            spec = spec,
            onClick = onLaunch,
            onLongClick = { menuFor = it },
        )

        Spacer(Modifier.height(8.dp))
        SectionLabel("ALL APPS")
        AllApps(
            apps = visible,
            spec = spec,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onClick = onLaunch,
            onLongClick = { menuFor = it },
        )
    }

    menuFor?.let { app ->
        val pinned = prefs.isPinned(app.key)
        AppMenu(
            app = app,
            pinned = pinned,
            canPin = prefs.canPinMore(),
            onPinToggle = {
                if (pinned) prefs.unpin(app.key) else prefs.pin(app.key)
                menuFor = null
            },
            onHide = {
                prefs.hide(app.key)
                menuFor = null
            },
            onInfo = {
                onAppInfo(app)
                menuFor = null
            },
            onDismiss = { menuFor = null },
        )
    }

    if (showSettings) {
        SettingsDialog(
            prefs = prefs,
            apps = apps,
            updateState = updateState,
            onCheckUpdate = onCheckUpdate,
            onOpenHomeSettings = onOpenHomeSettings,
            onDismiss = { showSettings = false },
        )
    }
}

// ---------------------------------------------------------------- top bar

@Composable
private fun TopBar(onSettings: () -> Unit) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000)
        }
    }
    val time = DateFormat.getTimeFormat(context).format(Date(now))
    val date = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(now))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(time, fontSize = 52.sp, fontWeight = FontWeight.Bold)
            Text(date, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onSettings)
                .padding(horizontal = 28.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

// ---------------------------------------------------------------- pinned + all apps

@Composable
private fun PinnedRow(
    apps: List<AppEntry>,
    spec: TileSpec,
    onClick: (AppEntry) -> Unit,
    onLongClick: (AppEntry) -> Unit,
) {
    if (apps.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(spec.tile * 0.6f),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                "Long-press any app below to pin it here.",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 6.dp),
    ) {
        items(apps, key = { it.key }) { app ->
            AppTile(
                entry = app,
                spec = spec,
                modifier = Modifier.size(width = spec.tile + 28.dp, height = spec.tile),
                onClick = { onClick(app) },
                onLongClick = { onLongClick(app) },
            )
        }
    }
}

@Composable
private fun AllApps(
    apps: List<AppEntry>,
    spec: TileSpec,
    modifier: Modifier,
    onClick: (AppEntry) -> Unit,
    onLongClick: (AppEntry) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val firstIndexOfLetter = remember(apps) {
        val map = LinkedHashMap<Char, Int>()
        apps.forEachIndexed { index, app ->
            if (!map.containsKey(letterOf(app.label))) map[letterOf(app.label)] = index
        }
        map
    }

    Row(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(spec.tile),
            state = gridState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(apps, key = { it.key }) { app ->
                AppTile(
                    entry = app,
                    spec = spec,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(spec.tile),
                    onClick = { onClick(app) },
                    onLongClick = { onLongClick(app) },
                )
            }
        }

        // A-Z rail: one tap jumps the grid, so there is never a long scroll while driving.
        Column(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight()
                .padding(start = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for ((letter, index) in firstIndexOfLetter) {
                Text(
                    text = letter.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { scope.launch { gridState.scrollToItem(index) } }
                        .padding(vertical = 2.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun letterOf(label: String): Char {
    val c = label.firstOrNull()?.uppercaseChar() ?: return '#'
    return if (c in 'A'..'Z') c else '#'
}

@Composable
private fun AppTile(
    entry: AppEntry,
    spec: TileSpec,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val iconPx = with(LocalDensity.current) { spec.icon.roundToPx() }
    val icon: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, entry.key, iconPx) {
        value = withContext(Dispatchers.Default) { IconCache.get(context, entry, iconPx) }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(spec.icon))
        } else {
            Spacer(Modifier.size(spec.icon))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = entry.label,
            fontSize = spec.label,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = spec.label * 1.15f,
        )
    }
}

// ---------------------------------------------------------------- dialogs

@Composable
private fun AppMenu(
    app: AppEntry,
    pinned: Boolean,
    canPin: Boolean,
    onPinToggle: () -> Unit,
    onHide: () -> Unit,
    onInfo: () -> Unit,
    onDismiss: () -> Unit,
) {
    val pinLabel = when {
        pinned -> "Unpin from home"
        canPin -> "Pin to home"
        else -> "Pinned row is full (${Prefs.MAX_PINNED})"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label, fontSize = 24.sp) },
        text = {
            Column {
                MenuButton(pinLabel, enabled = pinned || canPin, onClick = onPinToggle)
                MenuButton("Hide from list", onClick = onHide)
                MenuButton("App info", onClick = onInfo)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", fontSize = 18.sp) } },
    )
}

@Composable
private fun MenuButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Text(label, fontSize = 20.sp, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SettingsDialog(
    prefs: Prefs,
    apps: List<AppEntry>,
    updateState: UpdateState,
    onCheckUpdate: () -> Unit,
    onOpenHomeSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hiddenApps = apps.filter { it.key in prefs.hidden }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", fontSize = 26.sp) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SwitchRow("Extra-large tiles", prefs.extraLarge, prefs::updateExtraLarge)
                SwitchRow("Keep screen on", prefs.keepScreenOn, prefs::updateKeepScreenOn)
                SwitchRow("Hide system bars (swipe to show)", prefs.immersive, prefs::updateImmersive)

                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenHomeSettings, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Set DriveDeck as default home", fontSize = 18.sp)
                }

                if (hiddenApps.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("HIDDEN APPS")
                    hiddenApps.forEach { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(app.label, fontSize = 18.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { prefs.unhide(app.key) }) {
                                Text("Unhide", fontSize = 16.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                SectionLabel("UPDATES")
                Text("Version ${BuildConfig.VERSION_NAME}", fontSize = 18.sp)
                Text(
                    text = updateStatusText(updateState),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onCheckUpdate, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Check for updates", fontSize = 18.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", fontSize = 20.sp) } },
    )
}

private fun updateStatusText(state: UpdateState): String = when (state) {
    UpdateState.Idle -> ""
    UpdateState.Checking -> "Checking..."
    UpdateState.UpToDate -> "You're up to date."
    is UpdateState.Available -> "Update available: ${state.info.tag}"
    is UpdateState.Downloading -> "Downloading ${state.percent}%"
    is UpdateState.Failed -> state.message
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 18.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

// ---------------------------------------------------------------- update banner

@Composable
private fun UpdateBanner(state: UpdateState, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    when (state) {
        is UpdateState.Available -> BannerRow(
            message = "Update available: ${state.info.tag}",
            primaryLabel = "Update",
            onPrimary = onUpdate,
            secondaryLabel = "Later",
            onSecondary = onDismiss,
        )

        is UpdateState.Downloading -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                .padding(16.dp),
        ) {
            Text("Downloading update... ${state.percent}%", fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.percent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is UpdateState.Failed -> BannerRow(
            message = state.message,
            primaryLabel = "Dismiss",
            onPrimary = onDismiss,
        )

        else -> Unit
    }
}

@Composable
private fun BannerRow(
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, fontSize = 18.sp, modifier = Modifier.weight(1f))
        if (secondaryLabel != null) {
            TextButton(onClick = onSecondary) { Text(secondaryLabel, fontSize = 18.sp) }
        }
        Button(onClick = onPrimary) { Text(primaryLabel, fontSize = 18.sp) }
    }
}
