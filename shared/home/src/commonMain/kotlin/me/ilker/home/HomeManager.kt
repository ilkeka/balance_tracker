package me.ilker.home

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sdk.getLocalDate
import me.ilker.core.Manager
import me.ilker.core.extensions.round

class HomeManager(
    sdk: BalanceTrackerSDK
) : Manager<HomeState, HomeIntent, HomeSideEffect>() {
    override fun sendIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SetSelectedYearMonth -> handleSetSelectedYearMonth(intent.yearMonth)
        }
    }

    private fun handleSetSelectedYearMonth(yearMonth: YearMonth) {
        currentState.update {
            when (it) {
                HomeState.InitialState -> it
                is HomeState.Loaded -> it.copy(
                    selectedDate = LocalDate(
                        year = yearMonth.year,
                        month = yearMonth.month,
                        day = 1
                    )
                )
            }
        }
    }

    private val currentState: MutableStateFlow<HomeState> = MutableStateFlow(HomeState.InitialState)

    init {
        scope.launch {
            sdk.transactions.collect { transactions ->
                val transactionsByYearMonth = transactions
                    .groupBy { transaction -> transaction.getLocalDate().yearMonth }
                    .asIterable()
                    .sortedBy { it.key }
                    .associate { it.key to it.value.sortedBy { transaction -> transaction.dateTime } }

                val balances = transactionsByYearMonth.map { transactionByYearMonth ->
                    val (expense, income) = with(transactionByYearMonth.value.partition { it.type == TransactionType.Expense }) {
                        this.first.sumOf { transaction ->
                            transaction.amount
                        }.round(2) to
                                this.second.sumOf { transaction ->
                                    transaction.amount
                                }.round(2)
                    }
                    val balance = (income - expense).round(2)
                    val transactionsGropedByDateTime = transactionByYearMonth
                        .value
                        .sortedByDescending { it.id }
                        .take(3)
                        .groupBy { transaction -> transaction.getLocalDate() }

                    HomeState.Loaded.BalanceUiModel(
                        yearMonth = transactionByYearMonth.key,
                        balance = balance,
                        expense = expense,
                        income = income,
                        transactions = transactionsGropedByDateTime
                    )
                }

                currentState.update {
                    HomeState.Loaded(
                        selectedDate = currentState.value.selectedDate,
                        balances = balances
                    )
                }
            }
        }
    }

    override val state: StateFlow<HomeState> = currentState.asStateFlow()

    override val sideEffect: Channel<HomeSideEffect> = Channel(capacity = 1)
}
