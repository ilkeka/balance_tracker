package me.ilker.transaction.add

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.ilker.core.Manager

class AddTransactionManager : Manager<AddTransactionState, AddTransactionIntent, AddTransactionSideEffect>() {
    override fun sendIntent(intent: AddTransactionIntent) {
        when (intent) {
            is AddTransactionIntent.Add -> sideEffect.trySend(AddTransactionSideEffect.Feedback("Added ${intent.amount}"))
        }
    }

    private val managerState = MutableStateFlow(AddTransactionState.InitialState)
    override val state: StateFlow<AddTransactionState> = managerState.asStateFlow()

    override val sideEffect: Channel<AddTransactionSideEffect> = Channel()
}
