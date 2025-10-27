package me.ilker.transaction.add

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ilker.transaction.add.views.AddTransactionInitialView

@Composable
fun AddTransactionScreen() {
    val manager = remember { AddTransactionManager() }
    val state: State<AddTransactionState> = manager.state.collectAsStateWithLifecycle()

    when (val currentState = state.value) {
        AddTransactionState.InitialState -> AddTransactionInitialView()
    }
}
