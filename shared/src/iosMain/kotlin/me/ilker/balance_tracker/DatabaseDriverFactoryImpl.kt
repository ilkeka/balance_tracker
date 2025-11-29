package me.ilker.balance_tracker

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import me.ilker.balance_tracker.database.DatabaseDriverFactory

class DatabaseDriverFactoryImplDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return NativeSqliteDriver(Database.Schema, "Database.db")
    }
}
