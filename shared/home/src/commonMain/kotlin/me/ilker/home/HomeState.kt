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
    open val device: Device
) : State {
    /** Identifies this installation. There is no account; the device is the identity. */
    data class Device(
        val deviceId: String
    )

    data object InitialState: HomeState(
        selectedDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        device = Device(deviceId = "")
    )

    data class Loaded(
        override val selectedDate: LocalDate,
        override val device: Device,
        val balances: List<BalanceUiModel>
    ) : HomeState(
        selectedDate = selectedDate,
        device = device
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
