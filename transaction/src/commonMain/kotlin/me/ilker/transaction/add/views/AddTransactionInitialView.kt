package me.ilker.transaction.add.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.add
import me.ilker.balance_tracker.resources.new_transaction
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AddTransactionInitialView(
    snackbarHostState: SnackbarHostState,
    onAdd: (amount: Double) -> Unit
) {
    val inputState = rememberTextFieldState()

    Scaffold(
        modifier = Modifier.padding(12.dp),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = stringResource(Res.string.new_transaction)
                )
            }
        },
        bottomBar = {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                onClick = {
                    inputState.text.toString().toDoubleOrNull()?.let { amount ->
                        onAdd(amount)
                    }
                },
                enabled = inputState.text.isNotBlank(),
                content = {
                    Text(stringResource(Res.string.add))
                }
            )
        }
    ) { paddingValues ->
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                TextField(
                    state = inputState
                )
            }
        }
    }
}
