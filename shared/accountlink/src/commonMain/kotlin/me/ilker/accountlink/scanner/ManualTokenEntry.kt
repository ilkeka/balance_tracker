package me.ilker.accountlink.scanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.account_link_token_placeholder
import me.ilker.balance_tracker.resources.link_account
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ManualTokenEntry(
    enabled: Boolean,
    onScanned: (String) -> Unit
) {
    var token by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = token,
            onValueChange = { token = it },
            enabled = enabled,
            singleLine = true,
            placeholder = { Text(stringResource(Res.string.account_link_token_placeholder)) }
        )

        Spacer(Modifier.height(12.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { token.trim().takeIf { it.isNotBlank() }?.let(onScanned) },
            enabled = enabled && token.isNotBlank()
        ) {
            Text(stringResource(Res.string.link_account))
        }
    }
}
