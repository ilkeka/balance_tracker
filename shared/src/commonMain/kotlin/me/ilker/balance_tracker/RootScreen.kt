package me.ilker.balance_tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ilker.transaction.TransactionManager
import me.ilker.transaction.TransactionState

@Composable
fun RootScreen() {
    val manager = remember { TransactionManager() }
    val state: State<TransactionState> = manager.state.collectAsStateWithLifecycle()

    when (val currentState = state.value) {
        is TransactionState.Loaded -> LazyColumn(
            modifier = Modifier
                .safeContentPadding()
                .background(MaterialTheme.colorScheme.onError)
        ) {
//            items(currentState.transactions) { transaction ->
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    onClick = {}
//                ) {
//                    Text(
//                        modifier = Modifier.fillMaxWidth(),
//                        text = transaction.amount.toString()
//                    )
//                }
//
//            }
        }
    }
}
