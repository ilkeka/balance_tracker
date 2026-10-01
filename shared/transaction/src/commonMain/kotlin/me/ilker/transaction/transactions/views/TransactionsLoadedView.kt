package me.ilker.transaction.transactions.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.back
import me.ilker.balance_tracker.resources.balance
import me.ilker.balance_tracker.resources.expense_total
import me.ilker.balance_tracker.resources.income_total
import me.ilker.balance_tracker.resources.month_names
import me.ilker.balance_tracker.resources.nothing_yet
import me.ilker.balance_tracker.resources.start_create_transaction
import me.ilker.balance_tracker.resources.transactions
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sdk.getIcon
import me.ilker.balance_tracker.sdk.getValueForComposableUI
import me.ilker.core.extensions.toHumanReadableValue
import me.ilker.transaction.transactions.TransactionState
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

private val CardShape = RoundedCornerShape(24.dp)
private val CardPadding = 20.dp
private val ScreenPadding = 16.dp

@Composable
internal fun TransactionsLoadedView(
    state: TransactionState.Loaded,
    onClick: (id: Long) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { TransactionsTopBar(onBack = onBack) }
    ) { paddingValues ->
        val contentPadding = PaddingValues(
            start = ScreenPadding,
            end = ScreenPadding,
            top = paddingValues.calculateTopPadding() + 8.dp,
            bottom = paddingValues.calculateBottomPadding() + 16.dp
        )

        if (state.transactions.isEmpty()) {
            EmptyTransactions(modifier = Modifier.padding(contentPadding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.balance?.let { balance ->
                item {
                    BalanceCard(
                        balance = balance.balance,
                        income = balance.income,
                        expense = balance.expense
                    )
                }
            }

            state.transactions.forEach { (date, transactions) ->
                item(key = date) {
                    Text(
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        text = date.toHeaderText(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(
                    count = transactions.size,
                    key = { index -> transactions[index].id }
                ) { index ->
                    TransactionRow(
                        transaction = transactions[index],
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 48.dp)
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(Res.string.back)
            )
        }

        Text(
            text = stringResource(Res.string.transactions),
            fontSize = TextUnit(value = 24f, type = TextUnitType.Sp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BalanceCard(
    balance: Double,
    income: Double,
    expense: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(Res.string.balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = balance.toHumanReadableValue(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TotalStat(
                    label = stringResource(Res.string.income_total),
                    value = income,
                    modifier = Modifier.weight(1f)
                )

                TotalStat(
                    label = stringResource(Res.string.expense_total),
                    value = expense,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TotalStat(
    label: String,
    value: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = value.toHumanReadableValue(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyTransactions(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.nothing_yet),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(Res.string.start_create_transaction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionDomainModel,
    onClick: (Long) -> Unit
) {
    val isExpense = transaction.type == TransactionType.Expense
    val category = transaction.category.getValueForComposableUI()
    val description = transaction.description?.takeUnless { it.isBlank() }

    Card(
        onClick = { onClick(transaction.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = if (isExpense) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (isExpense) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = transaction.category.getIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = description ?: category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (description != null) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = (if (isExpense) "-" else "+") + transaction.amount.toHumanReadableValue(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

@Composable
private fun LocalDate.toHeaderText(): String {
    val monthNames = stringArrayResource(Res.array.month_names)

    return "$day ${format(
        format = LocalDate.Format {
            monthName(names = MonthNames(monthNames))
        }
    )}"
}
