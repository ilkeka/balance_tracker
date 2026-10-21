package me.ilker.sync.platform

import android.content.Context
import android.os.Build
import android.provider.Settings
import me.ilker.sync.transport.AndroidContextHolder

/**
 * "Galaxy S20 FE" rather than "SM-G780F": the name the user set in system settings is what they
 * recognise in the other phone's picker. It is readable without a permission; the model is only the
 * fallback for API levels that do not expose it.
 */
internal actual fun deviceDisplayName(): String =
    configuredDeviceName(AndroidContextHolder.context)
        ?: Build.MODEL.trim().takeIf { it.isNotEmpty() }
        ?: "Android"

internal actual fun deviceModel(): String =
    Build.MODEL.trim().takeIf { it.isNotEmpty() } ?: "Android"

private fun configuredDeviceName(context: Context?): String? {
    if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return null

    return runCatching {
        Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
    }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}
