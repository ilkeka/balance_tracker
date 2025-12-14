package me.ilker.balance_tracker.database

import app.cash.sqldelight.db.QueryResult
import me.ilker.balancetracker.User
import kotlin.Throws

interface DB {
    suspend fun createUser(
        id: String,
        email: String,
        password: String,
        createdAt: String
    ): QueryResult<Long>

    suspend fun getUser(
        id: String
    ): User?

    suspend fun getUserByMail(
        email: String
    ): User?

    @Throws(Exception::class)
    suspend fun getUsers(): List<User>
}
