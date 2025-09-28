package me.ilker.transaction

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.ilker.core.Manager

class TransactionManager : Manager<TransactionState, TransactionIntent, TransactionSideEffect>() {
    override fun sendIntent(intent: TransactionIntent) {
        TODO("Not yet implemented")
    }

    private val managerState = MutableStateFlow(
        TransactionState.Loaded(
            listOf(
                TransactionDomainModel(
                    id = "1",
                    amount = 10
                ),
                TransactionDomainModel(
                    id = "2",
                    amount = 5
                ),
                TransactionDomainModel(
                    id = "3",
                    amount = 15
                )
            )
        )
    )
    override val state: StateFlow<TransactionState> = managerState.asStateFlow()
}
