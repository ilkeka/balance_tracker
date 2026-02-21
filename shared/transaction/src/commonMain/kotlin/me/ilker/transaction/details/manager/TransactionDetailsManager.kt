package me.ilker.transaction.details.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.core.Manager
import me.ilker.transaction.details.TransactionDetailsIntent
import me.ilker.transaction.details.TransactionDetailsSideEffect
import me.ilker.transaction.details.TransactionDetailsState
import kotlin.coroutines.EmptyCoroutineContext

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
            else -> {}
        }
    }

    private val managerState: MutableStateFlow<TransactionDetailsState> = MutableStateFlow(TransactionDetailsState.InitialState)
    override val state: StateFlow<TransactionDetailsState> = managerState.asStateFlow()

    override val sideEffect: Channel<TransactionDetailsSideEffect> = Channel(capacity = 1)
}
