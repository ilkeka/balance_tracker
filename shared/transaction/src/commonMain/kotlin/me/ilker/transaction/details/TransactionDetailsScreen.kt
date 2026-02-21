package me.ilker.transaction.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import me.ilker.transaction.details.views.TransactionDetailsInitialView
import me.ilker.transaction.details.views.TransactionDetailsLoadedView

@Composable
fun TransactionDetailsScreen(
    state: State<TransactionDetailsState>,
    onBack: () -> Unit
) {
   when (val currentState = state.value) {
       is TransactionDetailsState.DetailsLoadedState -> TransactionDetailsLoadedView(
           state = currentState,
           onBack = onBack
       )
       TransactionDetailsState.InitialState -> TransactionDetailsInitialView(
           onBack = onBack
       )
   }
}
