package me.ilker.balance_tracker.sdk.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.api.apiClient
import me.ilker.balance_tracker.auth.AuthApi
import me.ilker.balance_tracker.auth.AuthRepository
import me.ilker.balance_tracker.auth.LinkApi
import me.ilker.balance_tracker.auth.SessionStorage
import me.ilker.balance_tracker.database.DB
import me.ilker.balance_tracker.database.DatabaseDriverFactory
import me.ilker.balance_tracker.sdk.AuthenticatedUser
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.TransactionType

internal class BalanceTrackerSDKImpl(
    driverFactory: DatabaseDriverFactory,
    sessionStorage: SessionStorage,
    baseUrl: String
) : BalanceTrackerSDK {
    private val database = DB(driverFactory)
    private val authRepository = AuthRepository(sessionStorage)
    private val client = apiClient(authRepository)
    private val authApi = AuthApi(client, baseUrl, authRepository)
    private val linkApi = LinkApi(client, baseUrl)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val authenticatedUser: StateFlow<AuthenticatedUser?> = authRepository.authenticatedUser

    override val sessionEmail: StateFlow<String?> = authRepository.authenticatedUser
        .map { it?.email }
        .stateIn(appScope, SharingStarted.Eagerly, null)

    init {
        appScope.launch {
            authRepository.restore()
        }
    }

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

    override suspend fun logout() {
        runCatching { authApi.logout() }
        authRepository.clear()
    }

    override suspend fun getLinkToken(): String = linkApi.getLinkToken()

    override suspend fun linkAccount(token: String) {
        linkApi.link(token)
    }
}
