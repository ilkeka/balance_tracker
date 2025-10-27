package me.ilker.transaction.add.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun AddTransactionInitialView() {
    val inputState = rememberTextFieldState()

    Scaffold(
        modifier = Modifier,
        topBar = {
            Text("Add New Transaction")
        },
        bottomBar = {

        }
    ) {
        Column(modifier = Modifier) {
            TextField(
                state = inputState
            )
        }
    }
}
