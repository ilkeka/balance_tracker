package me.ilker.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.month_names
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.core.Manager
import me.ilker.core.extensions.round
import org.jetbrains.compose.resources.getStringArray
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Clock

class HomeManager(
    sdk: BalanceTrackerSDK
) : Manager<HomeState, HomeIntent, HomeSideEffect>() {
    private val scope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

    override fun sendIntent(intent: HomeIntent) {
        when (intent) {
            else -> Unit
        }
    }

    private val currentState: MutableStateFlow<HomeState> = MutableStateFlow(HomeState.InitialState)

    override val state: StateFlow<HomeState> = sdk.transactions.map { transactions ->
        val transactionsByYearMonth = transactions.groupBy { transaction ->
            LocalDate.parse(
                input = transaction.dateTime,
                format = LocalDate.Format {
                    day()
                    char('/')
                    monthNumber()
                    char('/')
                    year()
                }
            ).yearMonth
        }

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
            val monthNames = getStringArray(Res.array.month_names)

            HomeState.Loaded.BalanceUiModel(
                selectedDate = transactionByYearMonth.key.format(
                    YearMonth.Format {
                        monthName(names = MonthNames(monthNames))
                        char(' ')
                        year()
                    }
                ),
                balance = balance,
                expense = expense,
                income = income
            )
        }

        val transactionsSorted = transactionsByYearMonth[Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.yearMonth]
            ?.sortedBy { it.dateTime }
            ?.takeLast(3)

        HomeState.Loaded(
            selectedDate = currentState.value.selectedDate,
            balances = balances,
            transactions = transactionsSorted
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Lazily,
        initialValue = currentState.value
    )

    override val sideEffect: Channel<HomeSideEffect> = Channel(capacity = 1)
}
