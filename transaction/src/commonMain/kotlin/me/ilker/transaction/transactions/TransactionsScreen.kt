package me.ilker.transaction.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import me.ilker.transaction.transactions.views.TransactionsLoadedView

@Composable
fun TransactionsScreen(
    state: State<TransactionState>,
    add: () -> Unit
) {
    when (val currentState = state.value) {
        is TransactionState.Loaded -> TransactionsLoadedView(
            transactions = currentState.transactions,
            add = add
        )
    }
}
