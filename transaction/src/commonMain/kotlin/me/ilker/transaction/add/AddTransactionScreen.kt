package me.ilker.transaction.add

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ilker.transaction.add.views.AddTransactionInitialView

@Composable
fun AddTransactionScreen() {
    val manager = remember { AddTransactionManager() }
    val state: State<AddTransactionState> = manager.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(manager.sideEffect) {
        val sideEffects = manager.sideEffect.receiveCatching()
        sideEffects.getOrNull()?.let { effect ->
            when (effect) {
                is AddTransactionSideEffect.Feedback -> snackbarHostState.showSnackbar(message = effect.text)
            }
        }
    }

    when (val currentState = state.value) {
        AddTransactionState.InitialState -> AddTransactionInitialView(
            snackbarHostState = snackbarHostState,
            add = { amount -> manager.sendIntent(AddTransactionIntent.Add(amount)) }
        )
    }
}
