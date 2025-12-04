package me.ilker.balance_tracker.sdk.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import me.ilker.balance_tracker.database.DB
import me.ilker.balance_tracker.database.DatabaseDriverFactory
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.transaction.transactions.TransactionDomainModel
import me.ilker.transaction.transactions.TransactionType

class BalanceTrackerSDKImpl(
    driverFactory: DatabaseDriverFactory
) : BalanceTrackerSDK {
    private val database = DB(driverFactory)

    override val transactions: Flow<List<TransactionDomainModel>> = database
        .getTransactions()
        .asFlow()
        .mapToList(Dispatchers.Default)

    @Throws(Exception::class)
    override suspend fun getTransactions() = database
        .getTransactions()
        .executeAsList()

    override suspend fun addTransaction(
        amount: Double,
        dateTime: String,
        type: TransactionType
    ) = database
        .addTransaction(
            amount = amount,
            dateTime = dateTime,
            type = type
        )
        .await()
}
