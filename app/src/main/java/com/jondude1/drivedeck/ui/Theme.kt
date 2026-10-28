package com.jondude1.drivedeck.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DriveScheme = darkColorScheme(
    primary = Color(0xFF5AB0FF),
    onPrimary = Color(0xFF00182E),
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFF2F5F8),
    surface = Color(0xFF161D27),
    onSurface = Color(0xFFF2F5F8),
    surfaceVariant = Color(0xFF1F2835),
    onSurfaceVariant = Color(0xFFA9B4C2),
    error = Color(0xFFFF6B6B),
)

/** Always dark, high contrast: easy to read at a glance and not blinding at night. */
@Composable
fun DriveDeckTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DriveScheme, content = content)
}
