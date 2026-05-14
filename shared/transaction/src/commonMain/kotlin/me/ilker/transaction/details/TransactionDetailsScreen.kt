package me.ilker.transaction.details

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.error_90001
import me.ilker.transaction.details.views.TransactionDetailsInitialView
import me.ilker.transaction.details.views.TransactionDetailsLoadedView
import org.jetbrains.compose.resources.getString

@Composable
fun TransactionDetailsScreen(
    state: State<TransactionDetailsState>,
    sideEffects: Flow<TransactionDetailsSideEffect>,
    onEdit: (id: Long) -> Unit,
    onEditClicked: (id: Long) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sideEffects) {
        sideEffects.collectLatest { effect ->
            when (effect) {
                TransactionDetailsSideEffect.Back -> onBack()
                is TransactionDetailsSideEffect.TransactionDeleted -> snackbarHostState.showSnackbar(message = effect.text)
                is TransactionDetailsSideEffect.Edit -> onEdit(effect.id)
                is TransactionDetailsSideEffect.ShowError -> snackbarHostState.showSnackbar(effect.code.message())
            }
        }
    }

    when (val currentState = state.value) {
       is TransactionDetailsState.DetailsLoadedState -> TransactionDetailsLoadedView(
           state = currentState,
           snackbarHostState = snackbarHostState,
           onDelete = onDelete,
           onBack = onBack,
           onEditClicked = onEditClicked
       )
       TransactionDetailsState.InitialState -> TransactionDetailsInitialView(
           onBack = onBack
       )
    }
}

private suspend fun TransactionDetailsSideEffect.ShowError.ErrorCode.message() = when (this) {
    TransactionDetailsSideEffect.ShowError.ErrorCode.NOT_FOUND -> getString(Res.string.error_90001, this.code)
}
