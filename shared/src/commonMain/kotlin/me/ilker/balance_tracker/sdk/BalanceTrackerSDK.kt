package me.ilker.balance_tracker.sdk

import kotlinx.coroutines.flow.Flow
import me.ilker.transaction.transactions.TransactionDomainModel

interface BalanceTrackerSDK {
    val transactions: Flow<List<TransactionDomainModel>>

    @Throws(Exception::class)
    suspend fun getTransactions(): List<TransactionDomainModel>

    @Throws(Exception::class)
    suspend fun addTransaction(amount: Double, dateTime: String): Long
}
