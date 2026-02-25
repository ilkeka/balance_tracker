package me.ilker.transaction.details

import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.core.State

sealed interface TransactionDetailsState: State {
    data object InitialState : TransactionDetailsState

    data class DetailsLoadedState(
        val transaction: TransactionDomainModel
    ): TransactionDetailsState
}
