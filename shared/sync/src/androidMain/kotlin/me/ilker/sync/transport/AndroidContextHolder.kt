package me.ilker.sync.transport

import android.content.Context

/**
 * The BLE transport needs a [Context] to open a GATT connection, but the transport interface is
 * deliberately context-free so the common sync layer stays platform agnostic. The sync screen records
 * the context it was composed with, which is the only place one is available.
 */
object AndroidContextHolder {
    var context: Context? = null
}
