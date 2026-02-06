package me.ilker.balance_tracker.database

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import me.ilker.balance_tracker.Database

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = Database.Schema.synchronous(),
        context = context,
        name = "Database.db"
    )
}
