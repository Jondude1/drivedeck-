package com.jondude1.drivedeck.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Small SharedPreferences wrapper whose values are Compose state, so the UI updates itself. */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.getSharedPreferences("drivedeck", Context.MODE_PRIVATE)

    /** Keys of apps pinned to the home row, in order. */
    var pinned: List<String> by mutableStateOf(
        (sp.getString(KEY_PINNED, "") ?: "").split("\n").filter { it.isNotBlank() }
    )
        private set

    var hidden: Set<String> by mutableStateOf(
        (sp.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()).toSet()
    )
        private set

    var extraLarge: Boolean by mutableStateOf(sp.getBoolean(KEY_XL, false))
        private set

    var keepScreenOn: Boolean by mutableStateOf(sp.getBoolean(KEY_KEEP_ON, true))
        private set

    var immersive: Boolean by mutableStateOf(sp.getBoolean(KEY_IMMERSIVE, false))
        private set

    var lastUpdateCheck: Long
        get() = sp.getLong(KEY_LAST_CHECK, 0L)
        set(value) {
            sp.edit().putLong(KEY_LAST_CHECK, value).apply()
        }

    fun isPinned(key: String): Boolean = key in pinned

    fun canPinMore(): Boolean = pinned.size < MAX_PINNED

    fun pin(key: String) {
        if (key !in pinned && canPinMore()) setPinned(pinned + key)
    }

    fun unpin(key: String) {
        setPinned(pinned - key)
    }

    fun hide(key: String) {
        unpin(key)
        setHidden(hidden + key)
    }

    fun unhide(key: String) {
        setHidden(hidden - key)
    }

    fun updateExtraLarge(value: Boolean) {
        extraLarge = value
        sp.edit().putBoolean(KEY_XL, value).apply()
    }

    fun updateKeepScreenOn(value: Boolean) {
        keepScreenOn = value
        sp.edit().putBoolean(KEY_KEEP_ON, value).apply()
    }

    fun updateImmersive(value: Boolean) {
        immersive = value
        sp.edit().putBoolean(KEY_IMMERSIVE, value).apply()
    }

    /** First run only: pin a sensible set of driving apps if they are installed. */
    fun ensureDefaultPins(installed: List<AppEntry>) {
        if (installed.isEmpty() || sp.getBoolean(KEY_PINNED_INIT, false)) return
        val picks = DEFAULT_PACKAGES
            .mapNotNull { pkg -> installed.firstOrNull { it.packageName == pkg }?.key }
            .take(6)
        setPinned(picks)
        sp.edit().putBoolean(KEY_PINNED_INIT, true).apply()
    }

    private fun setPinned(list: List<String>) {
        pinned = list
        sp.edit().putString(KEY_PINNED, list.joinToString("\n")).apply()
    }

    private fun setHidden(set: Set<String>) {
        hidden = set
        sp.edit().putStringSet(KEY_HIDDEN, HashSet(set)).apply()
    }

    companion object {
        const val MAX_PINNED = 8

        private const val KEY_PINNED = "pinned"
        private const val KEY_PINNED_INIT = "pinned_init"
        private const val KEY_HIDDEN = "hidden"
        private const val KEY_XL = "extra_large"
        private const val KEY_KEEP_ON = "keep_screen_on"
        private const val KEY_IMMERSIVE = "immersive"
        private const val KEY_LAST_CHECK = "last_update_check"

        private val DEFAULT_PACKAGES = listOf(
            "com.google.android.apps.maps",
            "com.waze",
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
        )
    }
}
