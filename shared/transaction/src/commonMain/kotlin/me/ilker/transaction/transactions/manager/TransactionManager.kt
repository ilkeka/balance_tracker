package me.ilker.transaction.transactions.manager

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sdk.getLocalDate
import me.ilker.core.Manager
import me.ilker.core.extensions.round
import me.ilker.transaction.transactions.TransactionIntent
import me.ilker.transaction.transactions.TransactionSideEffect
import me.ilker.transaction.transactions.TransactionState

class TransactionManager(
    sdk: BalanceTrackerSDK,
    yearMonth: String
) : Manager<TransactionState, TransactionIntent, TransactionSideEffect>() {
    private val transactionMonth: YearMonth = YearMonth.parse(yearMonth)

    override fun sendIntent(intent: TransactionIntent) {

    }

    override val state: StateFlow<TransactionState> = sdk
        .transactions
        .map { transactions ->
            val transactionsForMonth = transactions
                .filter { transaction -> transaction.getLocalDate().yearMonth == transactionMonth }
            val transactionsSorted = transactionsForMonth
                .sortedByDescending { it.id }
                .groupBy { transaction -> transaction.getLocalDate() }

            TransactionState.Loaded(
                yearMonth = transactionMonth,
                balance = run {
                    val (expense, income) = with(
                        transactionsForMonth.partition { it.type == TransactionType.Expense }
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
                categoryTotals = transactionsForMonth
                    .groupBy { transaction -> transaction.category to transaction.type }
                    .map { (categoryAndType, monthlyTransactions) ->
                        val (category, type) = categoryAndType

                        TransactionState.Loaded.CategoryTotalUiModel(
                            category = category,
                            type = type,
                            amount = monthlyTransactions.sumOf { transaction -> transaction.amount }.round(2)
                        )
                    }
                    .sortedWith(
                        compareBy(
                            { it.type != TransactionType.Expense },
                            { -it.amount }
                        )
                    ),
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
