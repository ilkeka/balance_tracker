package me.ilker.balance_tracker.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import me.ilker.balance_tracker.Database

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(
        schema = Database.Schema.synchronous(),
        name = "Database.db"
    )
}
