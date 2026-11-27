package me.ilker.transaction.transactions.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.balance
import me.ilker.balance_tracker.resources.breakdown
import me.ilker.balance_tracker.resources.expense_total
import me.ilker.balance_tracker.resources.income_total
import me.ilker.balance_tracker.resources.month_names
import me.ilker.balance_tracker.resources.no_expenses
import me.ilker.balance_tracker.resources.no_income
import me.ilker.balance_tracker.resources.nothing_yet
import me.ilker.balance_tracker.resources.start_create_transaction
import me.ilker.balance_tracker.resources.transactions_label
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sdk.getIcon
import me.ilker.balance_tracker.sdk.getLocalDate
import me.ilker.balance_tracker.sdk.getValueForComposableUI
import me.ilker.core.extensions.toHumanReadableValue
import me.ilker.transaction.common.CardPadding
import me.ilker.transaction.common.CardShape
import me.ilker.transaction.common.ScreenPadding
import me.ilker.transaction.common.TransactionTopBar
import me.ilker.transaction.transactions.TransactionState
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val PagerTransactionTypes = listOf(
    TransactionType.Expense,
    TransactionType.Income
)

private val SummaryPillShape = RoundedCornerShape(20.dp)

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

        val balance = state.balance ?: TransactionState.Loaded.BalanceUiModel(
            balance = 0.0,
            expense = 0.0,
            income = 0.0
        )
        val initialPage = remember(state.yearMonth) {
            if (balance.expense == 0.0 && balance.income > 0.0) 1 else 0
        }
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { PagerTransactionTypes.size }
        )
        val scope = rememberCoroutineScope()
        val selectedType = PagerTransactionTypes[pagerState.currentPage]

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MonthSummaryCard(
                balance = balance,
                selectedType = selectedType,
                onTypeSelected = { type ->
                    val page = PagerTransactionTypes.indexOf(type)

                    if (page >= 0 && page != pagerState.currentPage) {
                        scope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                    }
                },
                modifier = Modifier
                    .padding(horizontal = ScreenPadding)
                    .padding(top = 8.dp)
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                key = { page -> PagerTransactionTypes[page] },
                beyondViewportPageCount = 1
            ) { page ->
                val type = PagerTransactionTypes[page]
                val categoryTotals = state.categoryTotals.filter { categoryTotal ->
                    categoryTotal.type == type
                }
                val transactionsByCategory = remember(state.transactions, type) {
                    state.transactions.values
                        .flatten()
                        .filter { transaction -> transaction.type == type }
                        .groupBy { transaction -> transaction.category }
                }

                TypeTransactionsPage(
                    type = type,
                    total = categoryTotals.sumOf { categoryTotal -> categoryTotal.amount },
                    categoryTotals = categoryTotals,
                    transactionsByCategory = transactionsByCategory,
                    onClick = onClick
                )
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(
    balance: TransactionState.Loaded.BalanceUiModel,
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val balanceColor = when {
        balance.balance > 0.0 -> MaterialTheme.colorScheme.primary
        balance.balance < 0.0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier.fillMaxWidth(),
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
                    text = balance.balance.toHumanReadableValue(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PagerTransactionTypes.forEach { type ->
                    TypeSummaryPill(
                        type = type,
                        amount = if (type == TransactionType.Expense) {
                            balance.expense
                        } else {
                            balance.income
                        },
                        selected = type == selectedType,
                        onClick = { onTypeSelected(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeSummaryPill(
    type: TransactionType,
    amount: Double,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpense = type == TransactionType.Expense
    val accentColor = if (isExpense) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val containerColor = if (selected) {
        if (isExpense) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (selected) {
        if (isExpense) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .clip(SummaryPillShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            ),
        shape = SummaryPillShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    text = stringResource(
                        if (isExpense) Res.string.expense_total else Res.string.income_total
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = (if (isExpense) "-" else "+") + amount.toHumanReadableValue(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (selected) contentColor else accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TypeTransactionsPage(
    type: TransactionType,
    total: Double,
    categoryTotals: List<TransactionState.Loaded.CategoryTotalUiModel>,
    transactionsByCategory: Map<TransactionCategory, List<TransactionDomainModel>>,
    onClick: (id: Long) -> Unit
) {
    val transactionCount = transactionsByCategory.values.sumOf { transactionsForCategory ->
        transactionsForCategory.size
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = ScreenPadding,
            end = ScreenPadding,
            top = 12.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (categoryTotals.isNotEmpty()) {
            item(key = "breakdown") {
                TypeBreakdownCard(
                    type = type,
                    total = total,
                    transactionCount = transactionCount,
                    categoryTotals = categoryTotals,
                    transactionsByCategory = transactionsByCategory,
                    onClick = onClick
                )
            }
        }

        if (transactionsByCategory.isEmpty()) {
            item(key = "empty") {
                EmptyTransactionType(type = type)
            }
        }
    }
}

@Composable
private fun TypeBreakdownCard(
    type: TransactionType,
    total: Double,
    transactionCount: Int,
    categoryTotals: List<TransactionState.Loaded.CategoryTotalUiModel>,
    transactionsByCategory: Map<TransactionCategory, List<TransactionDomainModel>>,
    onClick: (id: Long) -> Unit
) {
    val isExpense = type == TransactionType.Expense
    val accentColor = if (isExpense) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val accentContainerColor = if (isExpense) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val accentContentColor = if (isExpense) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

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
                        text = stringResource(
                            if (isExpense) Res.string.expense_total else Res.string.income_total
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = (if (isExpense) "-" else "+") + total.toHumanReadableValue(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Spacer(Modifier.width(12.dp))

                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = accentContainerColor,
                    contentColor = accentContentColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isExpense) {
                                Icons.AutoMirrored.Rounded.TrendingDown
                            } else {
                                Icons.AutoMirrored.Rounded.TrendingUp
                            },
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            CategoryDonutChart(
                categoryTotals = categoryTotals,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 20.dp)
                    .size(196.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = transactionCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = pluralStringResource(
                            Res.plurals.transactions_label,
                            transactionCount
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = stringResource(Res.string.breakdown),
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            categoryTotals.forEachIndexed { index, categoryTotal ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                CategoryTotalRow(
                    categoryTotal = categoryTotal,
                    total = total,
                    transactions = transactionsByCategory[categoryTotal.category].orEmpty(),
                    onClick = onClick
                )
            }
        }
    }
}

@Composable
private fun CategoryTotalRow(
    categoryTotal: TransactionState.Loaded.CategoryTotalUiModel,
    total: Double,
    transactions: List<TransactionDomainModel>,
    onClick: (id: Long) -> Unit
) {
    val isExpense = categoryTotal.type == TransactionType.Expense
    val chartColor = categoryTotal.category.chartColor()
    val amountColor = if (isExpense) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val share = if (total > 0.0) {
        (categoryTotal.amount / total).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
    var expanded by rememberSaveable(categoryTotal.category.value) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "category chevron"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = transactions.isNotEmpty()) { expanded = !expanded }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = chartColor.copy(alpha = 0.18f),
                contentColor = chartColor
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

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = categoryTotal.category.getValueForComposableUI(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = (if (isExpense) "-" else "+") +
                            categoryTotal.amount.toHumanReadableValue(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )

                    Spacer(Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(chevronRotation),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { share },
                        modifier = Modifier.weight(1f),
                        color = chartColor,
                        trackColor = chartColor.copy(alpha = 0.15f)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "${(share * 100f).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                transactions.forEach { transaction ->
                    CategoryTransactionRow(
                        transaction = transaction,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTransactionRow(
    transaction: TransactionDomainModel,
    onClick: (Long) -> Unit
) {
    val isExpense = transaction.type == TransactionType.Expense
    val date = transaction.getLocalDate().toHeaderText()
    val description = transaction.description?.takeUnless { it.isBlank() }

    Surface(
        onClick = { onClick(transaction.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = description ?: date,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (description != null) {
                        FontWeight.Medium
                    } else {
                        FontWeight.Normal
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (description != null) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Text(
                text = (if (isExpense) "-" else "+") +
                    transaction.amount.toHumanReadableValue(),
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
}

@Composable
private fun EmptyTransactionType(type: TransactionType) {
    val isExpense = type == TransactionType.Expense
    val title = if (isExpense) {
        stringResource(Res.string.no_expenses)
    } else {
        stringResource(Res.string.no_income)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CardPadding, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
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
                        imageVector = if (isExpense) {
                            Icons.AutoMirrored.Rounded.TrendingDown
                        } else {
                            Icons.AutoMirrored.Rounded.TrendingUp
                        },
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(Res.string.start_create_transaction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
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
private fun LocalDate.toHeaderText(): String {
    val monthNames = stringArrayResource(Res.array.month_names)

    return "$day ${format(
        format = LocalDate.Format {
            monthName(names = MonthNames(monthNames))
        }
    )}"
}
