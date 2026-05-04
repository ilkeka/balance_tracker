package me.ilker.transaction.edit

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import me.ilker.transaction.edit.views.EditTransactionInitialView
import me.ilker.transaction.edit.views.EditTransactionLoadedView

@Composable
fun EditTransactionScreen(
    state: State<EditTransactionState>,
    sideEffects: Flow<EditTransactionSideEffect>,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sideEffects) {
        sideEffects.collectLatest { effect ->
            when (effect) {
                EditTransactionSideEffect.Back -> onBack()
                is EditTransactionSideEffect.TransactionDeleted -> snackbarHostState.showSnackbar(message = effect.text)
            }
        }
    }

    when (val currentState = state.value) {
       is EditTransactionState.TransactionLoadedState -> EditTransactionLoadedView(
           state = currentState,
           snackbarHostState = snackbarHostState,
           onDelete = onDelete,
           onBack = onBack
       )
        EditTransactionState.InitialState -> EditTransactionInitialView(
           onBack = onBack
       )
    }
}
