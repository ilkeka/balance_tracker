package me.ilker.transaction.transactions.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.yearMonth
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sdk.getLocalDate
import me.ilker.core.Manager
import me.ilker.core.extensions.round
import me.ilker.transaction.transactions.TransactionIntent
import me.ilker.transaction.transactions.TransactionSideEffect
import me.ilker.transaction.transactions.TransactionState
import kotlin.coroutines.EmptyCoroutineContext

class TransactionManager(
    sdk: BalanceTrackerSDK,
    yearMonth: String
) : Manager<TransactionState, TransactionIntent, TransactionSideEffect>() {
    private val scope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

    override fun sendIntent(intent: TransactionIntent) {

    }

    override val state: StateFlow<TransactionState> = sdk
        .transactions
        .map { transactions ->
            val transactionsSorted = transactions
                .filter { transaction -> transaction.getLocalDate().yearMonth.toString() == yearMonth }
                .sortedByDescending { it.id }
                .groupBy { transaction -> transaction.getLocalDate() }

            TransactionState.Loaded(
                balance = run {
                    val (expense, income) = with(
                        transactions.partition { it.type == TransactionType.Expense }
                    ) {
                        this.first.sumOf { transaction -> transaction.amount }.round(2) to
                        this.second.sumOf { transaction -> transaction.amount }.round(2)
                    }
                    val balance = (income - expense).round(2)

                    TransactionState.Loaded.BalanceUiModel(
                        balance = balance,
                        expense = expense,
                        income = income
                    )
                },
                transactions = transactionsSorted
            )
        }
        .stateIn(
        scope = scope,
        started = SharingStarted.Lazily,
        initialValue = TransactionState.InitialState
    )

    override val sideEffect: Channel<TransactionSideEffect> = Channel(capacity = 1)
}
