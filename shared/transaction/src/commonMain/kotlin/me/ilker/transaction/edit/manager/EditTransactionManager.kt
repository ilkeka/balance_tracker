package me.ilker.transaction.edit.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.delete_transaction_success_feedback
import me.ilker.balance_tracker.resources.edit_transaction_success_feedback
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.core.Manager
import me.ilker.transaction.edit.EditTransactionIntent
import me.ilker.transaction.edit.EditTransactionSideEffect
import me.ilker.transaction.edit.EditTransactionState
import org.jetbrains.compose.resources.getString
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class EditTransactionManager(
    private val id: Long,
    private val sdk: BalanceTrackerSDK
): Manager<EditTransactionState, EditTransactionIntent, EditTransactionSideEffect>() {
    private val scope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

    init {
        scope.launch {
            sdk
                .getTransactionById(id = id)
                ?.let { transactionDomainModel ->
                    managerState.update {
                        EditTransactionState.TransactionLoadedState(
                            transaction = transactionDomainModel
                        )
                    }
                }
        }
    }

    override fun sendIntent(intent: EditTransactionIntent) {
        when (intent) {
            EditTransactionIntent.DeleteTransaction -> deleteTransaction()
            is EditTransactionIntent.Edit -> editTransaction(
                amount = intent.amount,
                dateTime = intent.dateTime,
                type = intent.type,
                category = intent.category,
                description = intent.description
            )
        }
    }

    private val managerState: MutableStateFlow<EditTransactionState> = MutableStateFlow(EditTransactionState.InitialState)
    override val state: StateFlow<EditTransactionState> = managerState.asStateFlow()

    override val sideEffect: Channel<EditTransactionSideEffect> = Channel(capacity = 1)

    private fun deleteTransaction() {
        scope.launch {
            sdk.deleteTransaction(id = id)
            sideEffect.trySend(
                EditTransactionSideEffect.TransactionDeleted(
                    text = getString(Res.string.delete_transaction_success_feedback)
                )
            )
            delay(1.seconds)
            sideEffect.trySend(EditTransactionSideEffect.Back)
        }
    }

    private fun editTransaction(
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ) {
        scope.launch {
            val result = runCatching {
                sdk.editTransaction(
                    id = id,
                    amount = amount,
                    dateTime = dateTime,
                    type = type,
                    category = category,
                    description = description
                )
            }

            result.getOrNull()?.let {
                sideEffect.trySend(EditTransactionSideEffect.Feedback(getString(Res.string.edit_transaction_success_feedback)))
                delay(500.milliseconds)
                sideEffect.trySend(EditTransactionSideEffect.Back)
            }
        }
    }
}
