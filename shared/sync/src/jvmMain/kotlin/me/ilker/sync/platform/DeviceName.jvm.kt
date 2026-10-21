package me.ilker.sync.platform

import java.net.InetAddress

internal actual fun deviceDisplayName(): String =
    runCatching { InetAddress.getLocalHost().hostName }
        .getOrNull()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: deviceModel()

internal actual fun deviceModel(): String =
    System.getProperty("os.name").orEmpty().trim().takeIf { it.isNotEmpty() } ?: "Desktop"
