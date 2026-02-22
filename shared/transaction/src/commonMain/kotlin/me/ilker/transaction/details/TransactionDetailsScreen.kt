package me.ilker.transaction.details

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import me.ilker.transaction.details.views.TransactionDetailsInitialView
import me.ilker.transaction.details.views.TransactionDetailsLoadedView

@Composable
fun TransactionDetailsScreen(
    state: State<TransactionDetailsState>,
    sideEffects: Flow<TransactionDetailsSideEffect>,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sideEffects) {
        sideEffects.collectLatest { effect ->
            when (effect) {
                TransactionDetailsSideEffect.Back -> onBack()
                is TransactionDetailsSideEffect.TransactionDeleted -> snackbarHostState.showSnackbar(message = effect.text)
            }
        }
    }

    when (val currentState = state.value) {
       is TransactionDetailsState.DetailsLoadedState -> TransactionDetailsLoadedView(
           state = currentState,
           onDelete = onDelete,
           onBack = onBack
       )
       TransactionDetailsState.InitialState -> TransactionDetailsInitialView(
           onBack = onBack
       )
    }
}
