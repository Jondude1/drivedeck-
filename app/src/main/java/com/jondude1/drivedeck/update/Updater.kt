package com.jondude1.drivedeck.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val tag: String,
    val apkUrl: String,
    val apkName: String,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val percent: Int) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * Self-updater. Reads the latest GitHub Release of the public repo, compares the
 * trailing number of its tag (v1.0.<run>) with this build's versionCode, downloads
 * the .apk asset and hands it to the system installer.
 */
object Updater {
    private const val OWNER = "Jondude1"
    private const val REPO = "drivedeck-"
    private const val MAX_REDIRECTS = 6

    /** Blocking. Returns the newer release, or null when already up to date. */
    fun check(currentVersionCode: Int): UpdateInfo? {
        val body = httpGet("https://api.github.com/repos/$OWNER/$REPO/releases/latest")
        val release = JSONObject(body)
        val tag = release.getString("tag_name")
        val code = tag.substringAfterLast('.').filter { it.isDigit() }.toIntOrNull() ?: return null
        if (code <= currentVersionCode) return null

        val assets = release.getJSONArray("assets")
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.getString("name")
            if (name.endsWith(".apk", ignoreCase = true)) {
                return UpdateInfo(code, tag, asset.getString("browser_download_url"), name)
            }
        }
        return null
    }

    /** Opens the "install unknown apps" screen if needed. Returns true when installing is allowed. */
    fun ensureInstallPermission(context: Context): Boolean {
        if (context.packageManager.canRequestPackageInstalls()) return true
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return false
    }

    /** Blocking. Downloads the APK into the cache and reports 0-100 progress. */
    fun download(context: Context, info: UpdateInfo, onProgress: (Int) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "DriveDeck-update.apk")

        val conn = open(info.apkUrl, "application/octet-stream")
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Download failed (HTTP ${conn.responseCode})")
            }
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        return target
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun httpGet(url: String): String {
        val conn = open(url, "application/vnd.github+json")
        try {
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                throw IOException("No release found (is the repo public and has CI run?)")
            }
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** GitHub redirects downloads to a CDN on another host, so redirects are followed by hand. */
    private fun open(startUrl: String, accept: String): HttpURLConnection {
        var current = URL(startUrl)
        repeat(MAX_REDIRECTS) {
            val conn = current.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("User-Agent", "DriveDeck")
            conn.setRequestProperty("Accept", accept)
            val code = conn.responseCode
            if (code in intArrayOf(301, 302, 303, 307, 308)) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) throw IOException("Redirect without location")
                current = URL(current, location)
            } else {
                return conn
            }
        }
        throw IOException("Too many redirects")
    }
}
