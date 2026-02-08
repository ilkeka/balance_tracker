package me.ilker.transaction.transactions

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import me.ilker.transaction.transactions.views.TransactionsLoadedView

@ExperimentalMaterial3Api
@Composable
fun TransactionsScreen(
    state: State<TransactionState>,
    add: () -> Unit,
    onDeleteTransactions: () -> Unit,
    onDismissRequest: () -> Unit,
    onClick: (id: Long) -> Unit
) {
    when (val currentState = state.value) {
        is TransactionState.Loaded -> TransactionsLoadedView(
            state = currentState,
            add = add,
            onDeleteTransactions = onDeleteTransactions,
            onDismissRequest = onDismissRequest,
            onClick = onClick
        )
    }
}
