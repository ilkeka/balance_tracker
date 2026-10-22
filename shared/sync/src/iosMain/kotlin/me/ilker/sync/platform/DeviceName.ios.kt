package me.ilker.sync.platform

import platform.UIKit.UIDevice

internal actual fun deviceDisplayName(): String =
    UIDevice.currentDevice.name.trim().takeIf { it.isNotEmpty() }
        ?: UIDevice.currentDevice.model.trim().takeIf { it.isNotEmpty() }
        ?: "iPhone"

internal actual fun deviceModel(): String =
    UIDevice.currentDevice.model.trim().takeIf { it.isNotEmpty() } ?: "iPhone"
