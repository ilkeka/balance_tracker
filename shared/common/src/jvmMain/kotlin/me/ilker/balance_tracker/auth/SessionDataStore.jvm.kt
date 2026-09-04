package me.ilker.balance_tracker.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import okio.Path.Companion.toOkioPath

internal fun createSessionDataStore(): DataStore<Preferences> {
    val directory = File(System.getProperty("user.home"), ".balance_tracker").apply { mkdirs() }
    return PreferenceDataStoreFactory.createWithPath(
        produceFile = { File(directory, "session.preferences_pb").toOkioPath() }
    )
}
