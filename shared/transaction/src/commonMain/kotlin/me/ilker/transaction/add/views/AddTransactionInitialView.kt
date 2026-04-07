package me.ilker.transaction.add.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import kotlin.time.Instant
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.add
import me.ilker.balance_tracker.resources.amount
import me.ilker.balance_tracker.resources.amount_format
import me.ilker.balance_tracker.resources.back
import me.ilker.balance_tracker.resources.category
import me.ilker.balance_tracker.resources.date
import me.ilker.balance_tracker.resources.description
import me.ilker.balance_tracker.resources.expense
import me.ilker.balance_tracker.resources.income
import me.ilker.balance_tracker.resources.new_transaction
import me.ilker.balance_tracker.resources.transaction_type
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.core.extensions.round
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sdk.getValueForComposableUI
import me.ilker.balance_tracker.sdk.getValueForUI
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AddTransactionInitialView(
    snackbarHostState: SnackbarHostState,
    onAdd: (
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ) -> Unit,
    onBack: () -> Unit
) {
    val amountInputState = rememberTextFieldState()
    val categoryState: MutableState<TransactionCategory> = remember { mutableStateOf(TransactionCategory.Predefined.Other) }
    val categoryInputState = rememberTextFieldState()
    val categoryScrollState = rememberScrollState()
    val typeState = remember { mutableStateOf(TransactionType.Expense) }
    val typeInputState = rememberTextFieldState()
    var expandType by remember { mutableStateOf(false) }
    var expandCategory by remember { mutableStateOf(false) }
    var expandDate by remember { mutableStateOf(false) }
    var currentSelectedDateMillis by rememberSaveable { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentSelectedDateMillis)
    val descriptionState = rememberTextFieldState()
    val dateState by remember(currentSelectedDateMillis) {
        mutableStateOf(
            TextFieldState(
                with(
                    LocalDateTime.Format {
                        day()
                        char('/')
                        monthNumber()
                        char('/')
                        year()
                    }
                ) {
                    format(
                        Instant
                            .fromEpochMilliseconds(currentSelectedDateMillis)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                    )
                }
            )
        )
    }
    val submitEnabledState by remember(amountInputState) {
        derivedStateOf {
            amountInputState.text.isNotBlank() && amountInputState.text.toString()
                .toDoubleOrNull() != null
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
                    is PressInteraction.Press -> expandType = !expandType
                }

                interactions.emit(interaction)
            }

            override fun tryEmit(interaction: Interaction): Boolean {
                return interactions.tryEmit(interaction)
            }
        }
    }

    val categoryInteractionSource = remember {
        object : MutableInteractionSource {
            override val interactions = MutableSharedFlow<Interaction>(
                extraBufferCapacity = 16,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

            override suspend fun emit(interaction: Interaction) {
                when (interaction) {
                    is PressInteraction.Press -> expandCategory = !expandCategory
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
                    is PressInteraction.Press -> expandDate = !expandDate
                }

                interactions.emit(interaction)
            }

            override fun tryEmit(interaction: Interaction): Boolean {
                return interactions.tryEmit(interaction)
            }
        }
    }

    LaunchedEffect(currentSelectedDateMillis, datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { selectedDateMillis ->
            if (selectedDateMillis != currentSelectedDateMillis && expandDate) {
                expandDate = false
                currentSelectedDateMillis = selectedDateMillis
            }
        }
    }

    LaunchedEffect(typeState.value) {
        val typeString = when (typeState.value) {
            TransactionType.Expense -> getString(Res.string.expense)
            TransactionType.Income -> getString(Res.string.income)
        }
        typeInputState.edit {
            replace(0, length, typeString)
        }
    }

    LaunchedEffect(categoryState.value) {
        categoryInputState.edit {
            replace(0, length, categoryState.value.getValueForUI())
        }
    }

    Scaffold(
        modifier = Modifier,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .padding(top = 48.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(Res.string.back)
                    )
                }

                Text(
                    text = stringResource(Res.string.new_transaction),
                    fontSize = TextUnit(value = 24f, type = TextUnitType.Sp),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        bottomBar = {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                onClick = {
                    amountInputState.text.toString().toDoubleOrNull()?.round(2)?.let { amount ->
                        onAdd(
                            amount,
                            dateState.text.toString(),
                            typeState.value,
                            categoryState.value,
                            descriptionState.text.toString()
                        )
                    }
                },
                colors = ButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.33f),
                    disabledContainerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.33f),
                ),
                enabled = submitEnabledState,
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
                    ),
                    supportingText = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(Res.string.amount_format),
                            fontStyle = FontStyle.Normal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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

            if (expandDate) {
                item {
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandType = !expandType }
                ) {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        state = typeInputState,
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

            if (expandType) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        with(stringResource(Res.string.expense)) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        typeState.value = TransactionType.Expense
                                        expandType = !expandType
                                    }
                                    .padding(vertical = 4.dp),
                                text = this@with,
                                fontStyle = FontStyle.Italic
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.tertiary
                        )

                        with(stringResource(Res.string.income)) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        typeState.value = TransactionType.Income
                                        expandType = !expandType
                                    }
                                    .padding(vertical = 4.dp),
                                text = this@with,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandCategory = !expandCategory }
                ) {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        state = categoryInputState,
                        readOnly = true,
                        interactionSource = categoryInteractionSource,
                        placeholder = {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(Res.string.category),
                                fontStyle = FontStyle.Italic
                            )
                        }
                    )
                }
            }

            if (expandCategory) {
                item {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(categoryScrollState)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TransactionCategory.Predefined.entries.forEach { category ->
                            with(category.getValueForComposableUI()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            categoryState.value = category
                                            expandCategory = !expandCategory
                                        },
                                    colors = CardDefaults.cardColors(
                                        contentColor = MaterialTheme.colorScheme.tertiary,
                                        containerColor = MaterialTheme.colorScheme.onTertiary,
                                        disabledContentColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.33f),
                                        disabledContainerColor = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.33f),
                                    )
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .padding(4.dp),
                                        text = this@with,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                TextField(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    state = descriptionState,
                    placeholder = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(Res.string.description),
                            fontStyle = FontStyle.Italic
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    )
                )
            }
        }
    }
}
