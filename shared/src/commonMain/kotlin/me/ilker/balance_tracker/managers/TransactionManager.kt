package me.ilker.balance_tracker.managers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.core.Manager
import me.ilker.transaction.transactions.ModalBottomSheetState
import me.ilker.transaction.transactions.TransactionIntent
import me.ilker.transaction.transactions.TransactionSideEffect
import me.ilker.transaction.transactions.TransactionState
import me.ilker.transaction.transactions.TransactionType
import kotlin.coroutines.EmptyCoroutineContext

class TransactionManager(
    private val sdk: BalanceTrackerSDK
) : Manager<TransactionState, TransactionIntent, TransactionSideEffect>() {
    private val scope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

    private val modalState: MutableStateFlow<ModalBottomSheetState?> = MutableStateFlow(null)
    override fun sendIntent(intent: TransactionIntent) {
        when (intent) {
            is TransactionIntent.Add -> addTransaction(
                amount = intent.amount,
                dateTime = intent.dateTime,
                type = intent.type,
                description = intent.description
            )
            is TransactionIntent.OnClick -> onClick(intent.id)
            TransactionIntent.OnDismissRequest -> onDismissRequest()
        }
    }

    override val state: StateFlow<TransactionState> = combine(
        sdk.transactions,
        modalState
    ) { transactions, modalBottomSheetState ->
        TransactionState.Loaded(
            transactions = transactions,
            modalState = modalBottomSheetState
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.Lazily,
        initialValue = TransactionState.Loaded(
            transactions = emptyList(),
            modalState = null
        )
    )

    override val sideEffect: Channel<TransactionSideEffect> = Channel()

    private fun addTransaction(
        amount: Double,
        dateTime: String,
        type: TransactionType,
        description: String?
    ) {
        scope.launch {
            val result = runCatching {
                sdk.addTransaction(
                    amount = amount,
                    dateTime = dateTime,
                    type = type,
                    description = description
                )
            }
        }
    }

    private fun onClick(
        id: Long
    ) {
        val currentState = state.value as? TransactionState.Loaded ?: return

        currentState
            .transactions
            .find { transaction -> transaction.id == id }
            ?.let {
                modalState.update {
                    ModalBottomSheetState.ShowOptions
                }
            }
    }

    private fun onDismissRequest() {
        modalState.update { null }
    }
}
