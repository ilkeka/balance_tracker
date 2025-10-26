package me.ilker.transaction.add

import me.ilker.core.State
import me.ilker.transaction.TransactionDomainModel

sealed class AddTransactionState : State {
    object InitialState : AddTransactionState()
}
