package me.ilker.balance_tracker

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import me.ilker.balance_tracker.database.DatabaseDriverFactory

class DatabaseDriverFactoryImpl : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(url = "jdbc:sqlite:Database.db")
            .also {
                Database.Schema.create(it)
            }

        return driver
    }
}
