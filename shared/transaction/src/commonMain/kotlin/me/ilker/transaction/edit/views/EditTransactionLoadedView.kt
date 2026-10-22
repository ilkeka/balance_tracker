package me.ilker.transaction.edit.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.amount
import me.ilker.balance_tracker.resources.amount_format
import me.ilker.balance_tracker.resources.category
import me.ilker.balance_tracker.resources.date
import me.ilker.balance_tracker.resources.description
import me.ilker.balance_tracker.resources.edit_transaction
import me.ilker.balance_tracker.resources.expense
import me.ilker.balance_tracker.resources.income
import me.ilker.balance_tracker.resources.save
import me.ilker.balance_tracker.resources.transaction_type
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sdk.getLocalDate
import me.ilker.balance_tracker.sdk.getValueForComposableUI
import me.ilker.core.extensions.round
import me.ilker.transaction.common.CardPadding
import me.ilker.transaction.common.CardSpacing
import me.ilker.transaction.common.FormCard
import me.ilker.transaction.common.FormLabel
import me.ilker.transaction.common.ScreenPadding
import me.ilker.transaction.common.SelectRow
import me.ilker.transaction.common.TransactionTopBar
import me.ilker.transaction.edit.EditTransactionState
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditTransactionLoadedView(
    state: EditTransactionState.TransactionLoadedState,
    snackbarHostState: SnackbarHostState,
    onEdit: (
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ) -> Unit,
    onBack: () -> Unit
) {
    val amountInputState = rememberTextFieldState(state.transaction.amount.toString())
    val categoryState: MutableState<TransactionCategory> =
        remember(state.transaction.category) { mutableStateOf(state.transaction.category) }
    val typeState: MutableState<TransactionType> =
        remember(state.transaction.type) { mutableStateOf(state.transaction.type) }
    val descriptionState = rememberTextFieldState(state.transaction.description.orEmpty())
    var expandDate by remember { mutableStateOf(false) }
    var expandCategory by remember { mutableStateOf(false) }
    var currentSelectedDateMillis by rememberSaveable(state.transaction.dateTime) {
        val dateTime = LocalDateTime(
            date = state.transaction.getLocalDate(),
            time = LocalTime(hour = 0, minute = 0)
        )
        mutableStateOf(dateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds())
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = currentSelectedDateMillis)

    val selectedDateText = remember(currentSelectedDateMillis) {
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
    }

    val submitEnabledState by remember(amountInputState) {
        derivedStateOf {
            amountInputState.text.isNotBlank() && amountInputState.text.toString()
                .toDoubleOrNull() != null
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TransactionTopBar(
                title = stringResource(Res.string.edit_transaction),
                onBack = onBack
            )
        },
        bottomBar = {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding, vertical = CardPadding),
                onClick = {
                    amountInputState.text.toString().toDoubleOrNull()?.round(2)?.let { amount ->
                        onEdit(
                            amount,
                            selectedDateText,
                            typeState.value,
                            categoryState.value,
                            descriptionState.text.toString()
                        )
                    }
                },
                enabled = submitEnabledState,
                shape = RoundedCornerShape(16.dp),
                content = {
                    Icon(
                        imageVector = Icons.Rounded.Done,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(stringResource(Res.string.save))
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = ScreenPadding,
                end = ScreenPadding,
                top = 8.dp,
                bottom = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(CardSpacing)
        ) {
            item {
                FormCard {
                    FormLabel(text = stringResource(Res.string.transaction_type))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = typeState.value == TransactionType.Expense,
                            onClick = { typeState.value = TransactionType.Expense },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.errorContainer,
                                activeContentColor = MaterialTheme.colorScheme.onErrorContainer,
                                activeBorderColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(Res.string.expense))
                        }

                        SegmentedButton(
                            selected = typeState.value == TransactionType.Income,
                            onClick = { typeState.value = TransactionType.Income },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                activeBorderColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(stringResource(Res.string.income))
                        }
                    }
                }
            }

            item {
                FormCard {
                    FormLabel(text = stringResource(Res.string.amount))

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        state = amountInputState,
                        placeholder = {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(Res.string.amount),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = TextUnit(value = 32f, type = TextUnitType.Sp)
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        lineLimits = TextFieldLineLimits.SingleLine,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        supportingText = {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(Res.string.amount_format),
                                textAlign = TextAlign.Center
                            )
                        }
                    )
                }
            }

            item {
                FormCard {
                    FormLabel(text = stringResource(Res.string.date))

                    SelectRow(
                        icon = Icons.Rounded.DateRange,
                        value = selectedDateText,
                        expanded = expandDate,
                        onClick = { expandDate = !expandDate }
                    )

                    if (expandDate) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        DatePicker(
                            modifier = Modifier.fillMaxWidth(),
                            state = datePickerState,
                            showModeToggle = false,
                            title = null,
                            colors = DatePickerDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            item {
                FormCard {
                    FormLabel(text = stringResource(Res.string.category))

                    SelectRow(
                        icon = Icons.AutoMirrored.Rounded.List,
                        value = categoryState.value.getValueForComposableUI(),
                        expanded = expandCategory,
                        onClick = { expandCategory = !expandCategory }
                    )

                    if (expandCategory) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TransactionCategory.Predefined.entries.forEach { category ->
                                FilterChip(
                                    selected = categoryState.value == category,
                                    onClick = {
                                        categoryState.value = category
                                        expandCategory = false
                                    },
                                    label = { Text(category.getValueForComposableUI()) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                FormCard {
                    FormLabel(text = stringResource(Res.string.description))

                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        state = descriptionState,
                        placeholder = {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = stringResource(Res.string.description)
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
}
