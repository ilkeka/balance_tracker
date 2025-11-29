package me.ilker.balance_tracker.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.core.Manager
import me.ilker.transaction.transactions.TransactionIntent
import me.ilker.transaction.transactions.TransactionSideEffect
import me.ilker.transaction.transactions.TransactionState
import kotlin.coroutines.EmptyCoroutineContext

class TransactionManager(
    private val sdk: BalanceTrackerSDK
) : Manager<TransactionState, TransactionIntent, TransactionSideEffect>() {
    private val scope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

    override fun sendIntent(intent: TransactionIntent) {
        when (intent) {
            is TransactionIntent.Add -> addTransaction(intent.amount, "")
        }
    }

    override val state: StateFlow<TransactionState> = sdk
        .transactions
        .map { transactions -> TransactionState.Loaded(transactions) }
        .stateIn(scope, SharingStarted.Lazily, initialValue = TransactionState.Loaded(emptyList()))

    override val sideEffect: Channel<TransactionSideEffect> = Channel()

    private fun addTransaction(amount: Double, dateTime: String) {
        scope.launch {
            val result = runCatching {
                sdk.addTransaction(amount = amount, dateTime = dateTime)
            }
        }
    }
}
