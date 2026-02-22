package me.ilker.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.core.Manager
import me.ilker.core.extensions.round
import me.ilker.balance_tracker.sdk.TransactionType
import kotlin.coroutines.EmptyCoroutineContext

class HomeManager(
    sdk: BalanceTrackerSDK
) : Manager<HomeState, HomeIntent, HomeSideEffect>() {
    private val scope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

    private val modalState: MutableStateFlow<ModalBottomSheetState?> = MutableStateFlow(null)
    override fun sendIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.OnClick -> onClick(intent.id)
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

    override val sideEffect: Channel<HomeSideEffect> = Channel()

    private fun onClick(
        id: Long
    ) {
        val currentState = state.value as? HomeState.Loaded ?: return

        currentState
            .transactions
            .find { transaction -> transaction.id == id }
            ?.let {
                modalState.update {
                    ModalBottomSheetState.ShowOptions(
                        transactionId = id
                    )
                }
            }
    }
}
