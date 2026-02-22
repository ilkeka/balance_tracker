package me.ilker.transaction.transactions

import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.core.State

sealed class TransactionState : State {
    data object InitialState: TransactionState()

    data class Loaded(
        val balance: BalanceUiModel?,
        val transactions: List<TransactionDomainModel>
    ) : TransactionState() {
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
