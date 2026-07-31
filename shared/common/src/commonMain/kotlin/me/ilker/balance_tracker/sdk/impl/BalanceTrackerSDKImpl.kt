package me.ilker.balance_tracker.sdk.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import me.ilker.balance_tracker.auth.AuthApi
import me.ilker.balance_tracker.database.DB
import me.ilker.balance_tracker.database.DatabaseDriverFactory
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.TransactionType

class BalanceTrackerSDKImpl(
    driverFactory: DatabaseDriverFactory,
    baseUrl: String
) : BalanceTrackerSDK {
    private val database = DB(driverFactory)
    private val authApi = AuthApi(baseUrl)

    override val sessionEmail: StateFlow<String?> = authApi.sessionEmail

    override val transactions: Flow<List<TransactionDomainModel>> = database
        .getTransactions()
        .asFlow()
        .mapToList(Dispatchers.Default)

    override suspend fun getTransactionById(id: Long): TransactionDomainModel? = database
        .getTransactionById(id = id)
        .executeAsOneOrNull()

    @Throws(Exception::class)
    override suspend fun getTransactions() = database
        .getTransactions()
        .executeAsList()

    override suspend fun addTransaction(
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ) = database
        .addTransaction(
            amount = amount,
            dateTime = dateTime,
            type = type,
            category = category,
            description = description
        )

    override suspend fun editTransaction(
        id: Long,
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ) = database
        .editTransaction(
            id = id,
            amount = amount,
            dateTime = dateTime,
            type = type,
            category = category,
            description = description
        )

    override suspend fun deleteTransaction(id: Long) = database.deleteTransaction(
        id = id
    )

    override suspend fun authenticate(email: String, password: String) {
        authApi.authenticate(email, password)
    }
}
