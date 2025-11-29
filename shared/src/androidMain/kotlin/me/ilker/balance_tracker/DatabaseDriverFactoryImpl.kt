package me.ilker.balance_tracker

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import me.ilker.balance_tracker.database.DatabaseDriverFactory

class DatabaseDriverFactoryImpl(private val context: Context) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(Database.Schema, context, "Database.db")
    }
}
