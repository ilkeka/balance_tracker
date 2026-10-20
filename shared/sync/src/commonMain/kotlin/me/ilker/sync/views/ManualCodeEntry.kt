package me.ilker.sync.views

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

/**
 * A one-line field for a code that was copied from another device.
 *
 * Used both for pairing with a target that has no radio and for importing a document payload, so the
 * labels come from the caller instead of naming one of the two flows.
 */
@Composable
internal fun ManualCodeEntry(
    placeholder: String,
    action: String,
    enabled: Boolean,
    onSubmit: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = code,
            onValueChange = { code = it },
            enabled = enabled,
            singleLine = true,
            placeholder = { Text(placeholder) }
        )

        Spacer(Modifier.height(12.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { code.trim().takeIf { it.isNotBlank() }?.let(onSubmit) },
            enabled = enabled && code.isNotBlank()
        ) {
            Text(action)
        }
    }
}
