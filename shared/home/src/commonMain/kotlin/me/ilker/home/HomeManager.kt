package me.ilker.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.core.Manager
import me.ilker.core.extensions.round
import me.ilker.balance_tracker.sdk.TransactionType
import kotlin.coroutines.EmptyCoroutineContext

class HomeManager(
    sdk: BalanceTrackerSDK
) : Manager<HomeState, HomeIntent, HomeSideEffect>() {
    private val scope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

    override fun sendIntent(intent: HomeIntent) {
        when (intent) {
            else -> Unit
        }
    }

    override val state: StateFlow<HomeState> = sdk
        .transactions
        .map { transactions ->
        val transactionsSorted = transactions
            .sortedBy { it.dateTime }
            .takeLast(3)

        HomeState.Loaded(
            balance = run {
                val (expense, income) = with(transactions.partition { it.type == TransactionType.Expense }) {
                    this.first.sumOf { transaction -> transaction.amount }.round(2) to
                    this.second.sumOf { transaction -> transaction.amount }.round(2)
                }

                HomeState.Loaded.BalanceUiModel(
                    balance = income - expense,
                    expense = expense,
                    income = income
                )
            },
            transactions = transactionsSorted
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Lazily,
        initialValue = HomeState.InitialState
    )

    override val sideEffect: Channel<HomeSideEffect> = Channel(capacity = 1)
}
