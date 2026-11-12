package me.ilker.transaction.transactions.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.balance
import me.ilker.balance_tracker.resources.expense_total
import me.ilker.balance_tracker.resources.income_total
import me.ilker.balance_tracker.resources.month_names
import me.ilker.balance_tracker.resources.nothing_yet
import me.ilker.balance_tracker.resources.start_create_transaction
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sdk.getIcon
import me.ilker.balance_tracker.sdk.getValueForComposableUI
import me.ilker.core.extensions.round
import me.ilker.core.extensions.toHumanReadableValue
import me.ilker.transaction.common.CardPadding
import me.ilker.transaction.common.CardShape
import me.ilker.transaction.common.ScreenPadding
import me.ilker.transaction.common.TransactionTopBar
import me.ilker.transaction.transactions.TransactionState
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TransactionsLoadedView(
    state: TransactionState.Loaded,
    onClick: (id: Long) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TransactionTopBar(
                title = state.yearMonth.toMonthName(),
                onBack = onBack
            )
        }
    ) { paddingValues ->
        val contentPadding = PaddingValues(
            start = ScreenPadding,
            end = ScreenPadding,
            top = 8.dp,
            bottom = 16.dp
        )

        if (state.transactions.isEmpty()) {
            EmptyTransactions(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(contentPadding)
            )
            return@Scaffold
        }

        val expenseTotals = state.categoryTotals.filter { it.type == TransactionType.Expense }
        val incomeTotals = state.categoryTotals.filter { it.type == TransactionType.Income }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.balance?.let { balance ->
                item {
                    BalanceCard(
                        balance = balance.balance,
                        expenseTotals = expenseTotals,
                        incomeTotals = incomeTotals
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
private fun BalanceCard(
    balance: Double,
    expenseTotals: List<TransactionState.Loaded.CategoryTotalUiModel>,
    incomeTotals: List<TransactionState.Loaded.CategoryTotalUiModel>
) {
    val hasBreakdown = expenseTotals.isNotEmpty() || incomeTotals.isNotEmpty()
    var expanded by rememberSaveable { mutableStateOf(true) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "balance card chevron"
    )

    Card(
        onClick = { if (hasBreakdown) expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
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

                if (hasBreakdown) {
                    Spacer(Modifier.width(12.dp))

                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(chevronRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (hasBreakdown) {
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (expenseTotals.isNotEmpty()) {
                            CategoryTotalsSection(
                                title = stringResource(Res.string.expense_total),
                                isExpense = true,
                                categoryTotals = expenseTotals
                            )
                        }

                        if (incomeTotals.isNotEmpty()) {
                            CategoryTotalsSection(
                                title = stringResource(Res.string.income_total),
                                isExpense = false,
                                categoryTotals = incomeTotals
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTotalsSection(
    title: String,
    isExpense: Boolean,
    categoryTotals: List<TransactionState.Loaded.CategoryTotalUiModel>
) {
    val total = categoryTotals.sumOf { it.amount }.round(2)
    val amountColor = if (isExpense) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    var expanded by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = (if (isExpense) "-" else "+") + total.toHumanReadableValue(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categoryTotals.forEachIndexed { index, categoryTotal ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    CategoryTotalRow(categoryTotal = categoryTotal)
                }
            }
        }
    }
}

@Composable
private fun YearMonth.toMonthName(): String {
    val monthNames = stringArrayResource(Res.array.month_names)

    return format(
        YearMonth.Format {
            monthName(names = MonthNames(monthNames))
            char(' ')
            year()
        }
    )
}

@Composable
private fun CategoryTotalRow(
    categoryTotal: TransactionState.Loaded.CategoryTotalUiModel
) {
    val isExpense = categoryTotal.type == TransactionType.Expense

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
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
                    imageVector = categoryTotal.category.getIcon(),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = categoryTotal.category.getValueForComposableUI(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = (if (isExpense) "-" else "+") + categoryTotal.amount.toHumanReadableValue(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isExpense) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
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
                .padding(horizontal = CardPadding, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

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
