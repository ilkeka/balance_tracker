package me.ilker.transaction.transactions

import me.ilker.core.State

sealed class TransactionState : State {
    data class Loaded(
        val transactions: List<TransactionDomainModel>
    ) : TransactionState()
}
