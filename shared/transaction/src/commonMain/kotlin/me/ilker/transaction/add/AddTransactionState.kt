package me.ilker.transaction.add

import me.ilker.core.State

sealed interface AddTransactionState : State {
    object InitialState : AddTransactionState
}
