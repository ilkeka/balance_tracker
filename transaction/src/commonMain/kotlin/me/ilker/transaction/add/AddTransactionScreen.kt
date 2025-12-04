package me.ilker.transaction.add

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import me.ilker.transaction.add.views.AddTransactionInitialView
import me.ilker.transaction.transactions.TransactionType

@Composable
fun AddTransactionScreen(
    state: State<AddTransactionState>,
    sideEffects: Flow<AddTransactionSideEffect>,
    onAdd: (amount: Double, dateTime: String, type: TransactionType) -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sideEffects) {
        sideEffects.collectLatest { effect ->
            when (effect) {
                is AddTransactionSideEffect.Feedback -> snackbarHostState.showSnackbar(message = effect.text)
                AddTransactionSideEffect.Back -> onBack()
            }
        }
    }

    when (state.value) {
        AddTransactionState.InitialState -> AddTransactionInitialView(
            snackbarHostState = snackbarHostState,
            onAdd = { amount, dateTime, type -> onAdd(amount, dateTime, type) }
        )
    }
}
