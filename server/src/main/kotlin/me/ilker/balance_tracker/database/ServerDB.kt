package me.ilker.balance_tracker.database

import me.ilker.balance_tracker.User
import kotlin.Throws

interface ServerDB {
    suspend fun createUser(
        id: String,
        email: String,
        password: String,
        createdAt: String
    ): Long

    suspend fun getUser(
        id: String
    ): User?

    suspend fun getUserByEmail(
        email: String
    ): User?

    @Throws(Exception::class)
    suspend fun getUsers(): List<User>
}
