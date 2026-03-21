package me.ilker.balance_tracker.sdk

import kotlinx.coroutines.flow.Flow

interface BalanceTrackerSDK {
    val transactions: Flow<List<TransactionDomainModel>>

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

    suspend fun deleteTransaction(
        id: Long
    ): Long
}
