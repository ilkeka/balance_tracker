package me.ilker.home

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.core.State
import kotlin.time.Clock

sealed class HomeState(
    open val selectedDate: LocalDate,
    open val user: User
) : State {
    data class User(
        val sessionEmail: String?
    )

    data object InitialState: HomeState(
        selectedDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        user = User(sessionEmail = null)
    )

    data class Loaded(
        override val selectedDate: LocalDate,
        override val user: User,
        val balances: List<BalanceUiModel>
    ) : HomeState(
        selectedDate = selectedDate,
        user = user
    ) {
        data class BalanceUiModel(
            val yearMonth: YearMonth,
            val balance: Double,
            val expense: Double,
            val income: Double,
            val transactions: Map<LocalDate, List<TransactionDomainModel>>
        )
    }
}
