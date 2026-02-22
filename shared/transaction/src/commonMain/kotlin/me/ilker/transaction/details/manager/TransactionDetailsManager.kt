package me.ilker.transaction.details.manager

import balance_tracker.shared.transaction.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.resources.delete_transaction_success_feedback
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.core.Manager
import me.ilker.transaction.details.TransactionDetailsIntent
import me.ilker.transaction.details.TransactionDetailsSideEffect
import me.ilker.transaction.details.TransactionDetailsState
import org.jetbrains.compose.resources.getString
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.seconds

class TransactionDetailsManager(
    private val id: Long,
    private val sdk: BalanceTrackerSDK
): Manager<TransactionDetailsState, TransactionDetailsIntent, TransactionDetailsSideEffect>() {
    private val scope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

    init {
        scope.launch {
            sdk
                .getTransactionById(id = id)
                ?.let { transactionDomainModel ->
                    managerState.update {
                        TransactionDetailsState.DetailsLoadedState(
                            transaction = transactionDomainModel
                        )
                    }
                }
        }
    }

    override fun sendIntent(intent: TransactionDetailsIntent) {
        when (intent) {
            TransactionDetailsIntent.DeleteTransaction -> deleteTransaction()
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
}
