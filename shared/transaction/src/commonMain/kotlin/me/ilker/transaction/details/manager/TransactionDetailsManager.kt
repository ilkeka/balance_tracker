package me.ilker.transaction.details.manager

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.delete_transaction_success_feedback
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.core.Manager
import me.ilker.transaction.details.TransactionDetailsIntent
import me.ilker.transaction.details.TransactionDetailsSideEffect
import me.ilker.transaction.details.TransactionDetailsState
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.seconds

class TransactionDetailsManager(
    private val id: Long,
    private val sdk: BalanceTrackerSDK
): Manager<TransactionDetailsState, TransactionDetailsIntent, TransactionDetailsSideEffect>() {
    init {
        scope.launch {
            sdk
                .transactions
                .map { sdkTransactions -> sdkTransactions.firstOrNull { it.id == id } }
                .distinctUntilChanged()
                .collect { transaction ->
                    transaction
                        ?.let { transactionDomainModel ->
                            managerState.update {
                                TransactionDetailsState.DetailsLoadedState(
                                    transaction = transactionDomainModel
                                )
                            }
                        }
                }
        }
    }

    override fun sendIntent(intent: TransactionDetailsIntent) {
        when (intent) {
            TransactionDetailsIntent.DeleteTransaction -> deleteTransaction()
            is TransactionDetailsIntent.EditTransaction -> editTransaction(intent.id)
        }
    }

    private val managerState: MutableStateFlow<TransactionDetailsState> = MutableStateFlow(TransactionDetailsState.InitialState)
    override val state: StateFlow<TransactionDetailsState> = managerState.asStateFlow()

    override val sideEffect: Channel<TransactionDetailsSideEffect> = Channel(capacity = 1)

    private fun deleteTransaction() {
        scope.launch {
            sdk.deleteTransaction(id = id)
            sideEffect.trySend(
                TransactionDetailsSideEffect.TransactionDeleted(
                    text = getString(Res.string.delete_transaction_success_feedback)
                )
            )
            delay(1.seconds)
            sideEffect.trySend(TransactionDetailsSideEffect.Back)
        }
    }

    private fun editTransaction(id: Long) {
        scope.launch {
            sdk.getTransactionById(id = id)?.let {
                sideEffect.trySend(TransactionDetailsSideEffect.Edit(id = id))
            } ?: run {
                sideEffect.trySend(
                    TransactionDetailsSideEffect.ShowError(
                        code = TransactionDetailsSideEffect.ShowError.ErrorCode.NOT_FOUND
                    )
                )
            }
        }
    }
}
