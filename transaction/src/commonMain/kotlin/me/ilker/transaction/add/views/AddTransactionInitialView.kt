package me.ilker.transaction.add.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.time.Instant
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.add
import me.ilker.balance_tracker.resources.amount
import me.ilker.balance_tracker.resources.date
import me.ilker.balance_tracker.resources.expense
import me.ilker.balance_tracker.resources.income
import me.ilker.balance_tracker.resources.new_transaction
import me.ilker.balance_tracker.resources.transaction_type
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AddTransactionInitialView(
    snackbarHostState: SnackbarHostState,
    onAdd: (amount: Double) -> Unit,
) {
    val amountInputState = rememberTextFieldState()
    val expenseTypeInputState = rememberTextFieldState()
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val dateState by remember(datePickerState.selectedDateMillis) {
        derivedStateOf {
            TextFieldState(initialText = datePickerState.selectedDateMillis?.let { millis ->
                val localDateTime = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
                localDateTime.date.toString()
            } ?: "")
        }
    }

    val amountInteractionSource = remember {
        object : MutableInteractionSource {
            override val interactions = MutableSharedFlow<Interaction>(
                extraBufferCapacity = 16,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

            override suspend fun emit(interaction: Interaction) {
                when (interaction) {
                    is PressInteraction.Press -> expanded = !expanded
                }

                interactions.emit(interaction)
            }

            override fun tryEmit(interaction: Interaction): Boolean {
                return interactions.tryEmit(interaction)
            }
        }
    }

    val dateInteractionSource = remember {
        object : MutableInteractionSource {
            override val interactions = MutableSharedFlow<Interaction>(
                extraBufferCapacity = 16,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

            override suspend fun emit(interaction: Interaction) {
                when (interaction) {
                    is PressInteraction.Press -> showDatePicker = !showDatePicker
                }

                interactions.emit(interaction)
            }

            override fun tryEmit(interaction: Interaction): Boolean {
                return interactions.tryEmit(interaction)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .padding(vertical = 12.dp),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = stringResource(Res.string.new_transaction),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        bottomBar = {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                onClick = {
                    amountInputState.text.toString().toDoubleOrNull()?.let { amount ->
                        onAdd(amount)
                    }
                },
                enabled = amountInputState.text.isNotBlank(),
                content = {
                    Text(stringResource(Res.string.add))
                }
            )
        }
    ) { paddingValues ->
        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TextField(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    state = amountInputState,
                    placeholder = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(Res.string.amount),
                            fontStyle = FontStyle.Italic
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
            }

            item {
                TextField(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    state = dateState,
                    readOnly = true,
                    interactionSource = dateInteractionSource,
                    placeholder = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(Res.string.date),
                            fontStyle = FontStyle.Italic
                        )
                    }
                )
            }

            if (showDatePicker) {
                item {
                    DatePicker(
                        state = datePickerState,
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                ) {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        state = expenseTypeInputState,
                        readOnly = true,
                        interactionSource = amountInteractionSource,
                        placeholder = {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(Res.string.transaction_type),
                                fontStyle = FontStyle.Italic
                            )
                        }
                    )
                }
            }

            if (expanded) {
                item {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        with(stringResource(Res.string.expense)) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expenseTypeInputState.edit {
                                            this.replace(0, this.length, this@with)
                                        }
                                        expanded = !expanded
                                    },
                                text = this@with,
                                fontStyle = FontStyle.Italic
                            )
                        }

                        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)

                        with(stringResource(Res.string.income)) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expenseTypeInputState.edit {
                                            this.replace(0, this.length, this@with)
                                        }
                                        expanded = !expanded
                                    },
                                text = this@with,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }
}
