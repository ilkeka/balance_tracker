package me.ilker.transaction.add

import me.ilker.core.State

sealed class AddTransactionState : State {
    object InitialState : AddTransactionState()
}
