package me.ilker.balance_tracker.sdk

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BalanceTrackerSDK {
    val transactions: Flow<List<TransactionDomainModel>>
    val authenticatedUser: StateFlow<AuthenticatedUser?>
    val sessionEmail: StateFlow<String?>

    suspend fun getTransactionById(id: Long): TransactionDomainModel?

    @Throws(Exception::class)
    suspend fun getTransactions(): List<TransactionDomainModel>

    @Throws(Exception::class)
    suspend fun addTransaction(
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ): Long

    @Throws(Exception::class)
    suspend fun editTransaction(
        id: Long,
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ): Long

    suspend fun deleteTransaction(
        id: Long
    ): Long

    @Throws(Exception::class)
    suspend fun authenticate(email: String, password: String)

    @Throws(Exception::class)
    suspend fun getLinkToken(): String

    @Throws(Exception::class)
    suspend fun linkAccount(token: String)
}
