package me.ilker.transaction.transactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ilker.transaction.transactions.views.TransactionsLoadedView

@Composable
fun TransactionsScreen(
    add: () -> Unit
) {
    val manager = remember { TransactionManager() }
    val state: State<TransactionState> = manager.state.collectAsStateWithLifecycle()

    when (val currentState = state.value) {
        is TransactionState.Loaded -> TransactionsLoadedView(
            transactions = currentState.transactions,
            add = add
        )
    }
}
