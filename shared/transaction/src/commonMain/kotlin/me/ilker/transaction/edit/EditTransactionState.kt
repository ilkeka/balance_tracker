package me.ilker.transaction.edit

import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.core.State

sealed interface EditTransactionState: State {
    data object InitialState : EditTransactionState

    data class TransactionLoadedState(
        val transaction: TransactionDomainModel
    ): EditTransactionState
}
