package me.ilker.balance_tracker.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.core.Manager
import me.ilker.transaction.add.AddTransactionIntent
import me.ilker.transaction.add.AddTransactionSideEffect
import me.ilker.transaction.add.AddTransactionState
import kotlin.coroutines.EmptyCoroutineContext

class AddTransactionManager(
    private val sdk: BalanceTrackerSDK
) : Manager<AddTransactionState, AddTransactionIntent, AddTransactionSideEffect>() {
    private val scope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

    override fun sendIntent(intent: AddTransactionIntent) {
        when (intent) {
            is AddTransactionIntent.Add -> addTransaction(
                amount = intent.amount,
                dateTime = intent.dateTime
            )
        }
    }

    private val managerState = MutableStateFlow(AddTransactionState.InitialState)
    override val state: StateFlow<AddTransactionState> = managerState.asStateFlow()

    override val sideEffect: Channel<AddTransactionSideEffect> = Channel()

    private fun addTransaction(amount: Double, dateTime: String) {
        scope.launch {
            val result = runCatching {
                sdk.addTransaction(amount = amount, dateTime = dateTime)
            }

            result.getOrNull()?.let {
                sideEffect.trySend(AddTransactionSideEffect.Feedback("Added transaction with id: $it"))
                delay(500)
                sideEffect.trySend(AddTransactionSideEffect.Back)
            }
        }
    }
}
