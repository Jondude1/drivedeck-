package com.jondude1.drivedeck.data

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap

/** Renders each app icon once per size and keeps it in memory. */
object IconCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun get(context: Context, entry: AppEntry, sizePx: Int): ImageBitmap {
        return cache.getOrPut("${entry.key}@$sizePx") {
            val pm = context.packageManager
            val drawable = try {
                pm.getActivityIcon(entry.component)
            } catch (e: Exception) {
                pm.defaultActivityIcon
            }
            drawable.toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).asImageBitmap()
        }
    }
}
