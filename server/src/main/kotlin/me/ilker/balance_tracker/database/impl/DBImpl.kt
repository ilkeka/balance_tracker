package me.ilker.balance_tracker.database.impl

import me.ilker.balance_tracker.Database
import me.ilker.balance_tracker.database.DB
import me.ilker.balance_tracker.database.DatabaseDriverFactory
import kotlin.Throws

internal class DBImpl(
    databaseDriverFactory: DatabaseDriverFactory
): DB {
    private val database = Database.Companion(
        driver = databaseDriverFactory.createDriver()
    )

    private val dbQuery = database.databaseQueries

    override suspend fun createUser(
        id: String,
        email: String,
        password: String,
        createdAt: String
    ) = dbQuery
        .insertUser(
            id = id,
            email = email,
            password = password,
            createdAt = createdAt
        )

    override suspend fun getUser(
        id: String
    ) = dbQuery
        .getUser(id = id)
        .executeAsOneOrNull()

    override suspend fun getUserByMail(
        email: String
    ) = dbQuery
        .getUserByEmail(email = email)
        .executeAsOneOrNull()

    @Throws(Exception::class)
    override suspend fun getUsers() = dbQuery
        .getUsers()
        .executeAsList()
}
