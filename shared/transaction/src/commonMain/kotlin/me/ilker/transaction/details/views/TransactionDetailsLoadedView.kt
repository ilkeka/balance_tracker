package me.ilker.transaction.details.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.amount
import me.ilker.balance_tracker.resources.back
import me.ilker.balance_tracker.resources.category
import me.ilker.balance_tracker.resources.date
import me.ilker.balance_tracker.resources.delete
import me.ilker.balance_tracker.resources.description
import me.ilker.balance_tracker.resources.description_not_found
import me.ilker.balance_tracker.resources.expense
import me.ilker.balance_tracker.resources.ic_edit
import me.ilker.balance_tracker.resources.income
import me.ilker.balance_tracker.resources.transaction_details
import me.ilker.balance_tracker.resources.transaction_type
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sdk.getValueForComposableUI
import me.ilker.core.extensions.toHumanReadableValue
import me.ilker.transaction.details.TransactionDetailsState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TransactionDetailsLoadedView(
    state: TransactionDetailsState.DetailsLoadedState,
    snackbarHostState: SnackbarHostState,
    onEditClicked: (id: Long) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
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
                    text = stringResource(Res.string.transaction_details),
                    fontSize = TextUnit(value = 24f, type = TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        bottomBar = {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                onClick = onDelete,
                colors = ButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.33f),
                    disabledContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.33f),
                ),
                content = {
                    Text(
                        text = stringResource(Res.string.delete)
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                ) {
                    Row {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.amount),
                                fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiary
                            )

                            Text(
                                text = state.transaction.amount.toHumanReadableValue(),
                                fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }

                        IconButton(
                            modifier = Modifier.size(48.dp),
                            onClick = { onEditClicked(state.transaction.id) },
                            content = {
                                Icon(
                                    tint = MaterialTheme.colorScheme.primaryContainer,
                                    painter = painterResource(Res.drawable.ic_edit),
                                    contentDescription = null
                                )
                            }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(Res.string.transaction_type),
                        fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiary
                    )

                    Text(
                        text = when (state.transaction.type) {
                            TransactionType.Expense -> stringResource(Res.string.expense)
                            TransactionType.Income -> stringResource(Res.string.income)
                        },
                        fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onTertiary
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(Res.string.date),
                        fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiary
                    )

                    Text(
                        text = state.transaction.dateTime,
                        fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onTertiary
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(Res.string.category),
                        fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiary
                    )

                    Text(
                        text = state.transaction.category.getValueForComposableUI(),
                        fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onTertiary
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(Res.string.description),
                        fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiary
                    )

                    Text(
                        text = state.transaction.description?.takeUnless { it.isBlank() } ?: stringResource(Res.string.description_not_found),
                        fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                }
            }
        }
    }
}
