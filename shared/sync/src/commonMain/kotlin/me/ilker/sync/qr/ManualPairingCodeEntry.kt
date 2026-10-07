package me.ilker.sync.qr

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.sync_pair_button
import me.ilker.balance_tracker.resources.sync_pair_code_placeholder
import org.jetbrains.compose.resources.stringResource

/**
 * Fallback used on targets with no camera, matching how the scanner already degrades on desktop and
 * web. The pairing code is short enough to read across a desk or paste from a message.
 */
@Composable
internal fun ManualPairingCodeEntry(
    enabled: Boolean,
    onScanned: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = code,
            onValueChange = { code = it },
            enabled = enabled,
            singleLine = true,
            placeholder = { Text(stringResource(Res.string.sync_pair_code_placeholder)) }
        )

        Spacer(Modifier.height(12.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { code.trim().takeIf { it.isNotBlank() }?.let(onScanned) },
            enabled = enabled && code.isNotBlank()
        ) {
            Text(stringResource(Res.string.sync_pair_button))
        }
    }
}