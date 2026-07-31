package me.ilker.balance_tracker.database

import me.ilker.balance_tracker.AccountLink
import me.ilker.balance_tracker.LinkToken
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

    suspend fun createLinkToken(
        token: String,
        ownerId: String,
        createdAt: String
    ): Long

    suspend fun getLinkToken(
        token: String
    ): LinkToken?

    suspend fun deleteLinkToken(
        token: String
    )

    suspend fun createAccountLink(
        id: String,
        ownerId: String,
        linkedId: String,
        createdAt: String
    ): Long

    suspend fun getAccountLink(
        ownerId: String,
        linkedId: String
    ): AccountLink?
}
