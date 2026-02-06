package me.ilker.balance_tracker.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver
import me.ilker.balance_tracker.Database

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = createDefaultWebWorkerDriver().also {
        Database.Schema.create(it)
    }
}
