package me.ilker.transaction

import me.ilker.core.State

sealed class TransactionState : State {
    data class Loaded(
        val transactions: List<TransactionDomainModel>
    ) : TransactionState()
}
