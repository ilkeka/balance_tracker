package me.ilker.transaction.add

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.ilker.core.Manager

class AddTransactionManager : Manager<AddTransactionState, AddTransactionIntent, AddTransactionSideEffect>() {
    override fun sendIntent(intent: AddTransactionIntent) {
        TODO("Not yet implemented")
    }

    private val managerState = MutableStateFlow(AddTransactionState.InitialState)
    override val state: StateFlow<AddTransactionState> = managerState.asStateFlow()
}
