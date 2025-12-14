package me.ilker.balance_tracker

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.ilker.balance_tracker.database.DatabaseDriverFactory
import java.util.Properties

class DatabaseDriverFactoryImpl : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        return JdbcSqliteDriver(
            url = "jdbc:sqlite:Database.db",
            properties = Properties(),
            schema = Database.Schema
        )
    }
}
