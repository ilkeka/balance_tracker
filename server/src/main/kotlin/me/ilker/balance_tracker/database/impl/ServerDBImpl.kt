package me.ilker.balance_tracker.database.impl

import app.cash.sqldelight.EnumColumnAdapter
import me.ilker.balance_tracker.Database
import me.ilker.balance_tracker.database.DatabaseDriverFactory
import me.ilker.balance_tracker.database.ServerDB
import me.ilker.balancetracker.Transactions
import kotlin.Throws

internal class ServerDBImpl(
    databaseDriverFactory: DatabaseDriverFactory
): ServerDB {
    private val database = Database(
        driver = databaseDriverFactory.createDriver(),
        TransactionsAdapter = Transactions.Adapter(
            typeAdapter =  EnumColumnAdapter(),
        ),
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
