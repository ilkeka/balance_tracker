package me.ilker.balance_tracker

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.createDefaultWebWorkerDriver
import me.ilker.balance_tracker.database.DatabaseDriverFactory

class DatabaseDriverFactoryImpl : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return createDefaultWebWorkerDriver()
    }
}
