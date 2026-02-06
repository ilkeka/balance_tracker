package me.ilker.balance_tracker.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.ilker.balance_tracker.Database

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = JdbcSqliteDriver(
        url = "jdbc:sqlite:Database.db",
        schema = Database.Schema.synchronous()
    )
}
