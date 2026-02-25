package me.ilker.home

import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.core.State

sealed class HomeState : State {
    data object InitialState: HomeState()

    data class Loaded(
        val balance: BalanceUiModel?,
        val transactions: List<TransactionDomainModel>
    ) : HomeState() {
        data class BalanceUiModel(
            val balance: Double,
            val expense: Double,
            val income: Double,
        )
    }
}

sealed class ModalBottomSheetState {
    data class ShowOptions(
        val transactionId: Long
    ) : ModalBottomSheetState()
}
