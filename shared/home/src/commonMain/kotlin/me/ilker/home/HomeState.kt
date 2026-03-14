package me.ilker.home

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.core.State
import kotlin.time.Clock

sealed class HomeState(
    open val selectedDate: LocalDate
) : State {
    data object InitialState: HomeState(
        selectedDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    )

    data class Loaded(
        override val selectedDate: LocalDate,
        val balances: List<BalanceUiModel>
    ) : HomeState(
        selectedDate = selectedDate
    ) {
        data class BalanceUiModel(
            val selectedDate: String,
            val balance: Double,
            val expense: Double,
            val income: Double,
            val transactions: List<TransactionDomainModel>
        )
    }
}
