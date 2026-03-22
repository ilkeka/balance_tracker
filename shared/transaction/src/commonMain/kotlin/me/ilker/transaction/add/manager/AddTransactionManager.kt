package me.ilker.transaction.add.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.add_transaction_success_feedback
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.core.Manager
import me.ilker.transaction.add.AddTransactionIntent
import me.ilker.transaction.add.AddTransactionSideEffect
import me.ilker.transaction.add.AddTransactionState
import org.jetbrains.compose.resources.getString
import kotlin.coroutines.EmptyCoroutineContext

class AddTransactionManager(
    private val sdk: BalanceTrackerSDK
) : Manager<AddTransactionState, AddTransactionIntent, AddTransactionSideEffect>() {
    private val scope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

    override fun sendIntent(intent: AddTransactionIntent) {
        when (intent) {
            is AddTransactionIntent.Add -> addTransaction(
                amount = intent.amount,
                dateTime = intent.dateTime,
                type = intent.type,
                category = intent.category,
                description = intent.description
            )
        }
    }

    private val managerState = MutableStateFlow(AddTransactionState.InitialState)
    override val state: StateFlow<AddTransactionState> = managerState.asStateFlow()

    override val sideEffect: Channel<AddTransactionSideEffect> = Channel(capacity = 1)

    private fun addTransaction(
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ) {
        scope.launch {
            val result = runCatching {
                sdk.addTransaction(
                    amount = amount,
                    dateTime = dateTime,
                    type = type,
                    category = category,
                    description = description
                )
            }

            result.getOrNull()?.let {
                sideEffect.trySend(AddTransactionSideEffect.Feedback(getString(Res.string.add_transaction_success_feedback)))
                delay(500)
                sideEffect.trySend(AddTransactionSideEffect.Back)
            }
        }
    }
}