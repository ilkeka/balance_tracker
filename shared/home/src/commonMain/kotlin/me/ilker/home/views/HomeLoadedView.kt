package me.ilker.home.views

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.add
import me.ilker.balance_tracker.resources.balance
import me.ilker.balance_tracker.resources.expense_total
import me.ilker.balance_tracker.resources.income_total
import me.ilker.balance_tracker.resources.latest_transactions
import me.ilker.balance_tracker.resources.month_names
import me.ilker.balance_tracker.resources.see_all
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sdk.getIcon
import me.ilker.balance_tracker.sdk.getValueForComposableUI
import me.ilker.core.extensions.toHumanReadableValue
import me.ilker.home.HomeState
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeLoadedView(
    state: HomeState.Loaded,
    device: HomeState.Device,
    setSelectedYearMonth: (yearMonth: YearMonth) -> Unit,
    add: () -> Unit,
    onTransactionsClicked: () -> Unit,
    onClick: (id: Long) -> Unit,
    onSync: () -> Unit = {}
) {
    val balancePagerState = rememberPagerState(
        initialPage = state.balances.lastIndex.takeUnless { it < 0 } ?: 0,
        pageCount = { state.balances.size }
    )

    LaunchedEffect(balancePagerState) {
        snapshotFlow { balancePagerState.currentPage }.collect { page ->
            state.balances.getOrNull(page)?.yearMonth?.let {
                setSelectedYearMonth(it)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeTopBar(
                device = device,
                onSync = onSync
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = add,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(Res.string.add)
                )
            }
        }
    ) { paddingValues ->
        val balances = state.balances

        balances.takeUnless { it.isEmpty() }?.let {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = HomeScreenPadding,
                    end = HomeScreenPadding,
                    top = 8.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    HorizontalPager(
                        modifier = Modifier.fillMaxWidth(),
                        state = balancePagerState,
                        verticalAlignment = Alignment.Top,
                        pageSize = PageSize.Fill
                    ) { page ->
                        BalanceCard(
                            balance = balances[page],
                            onClick = onTransactionsClicked
                        )
                    }
                }

                if (balances.size > 1) {
                    item {
                        PageIndicator(
                            count = balances.size,
                            currentPage = balancePagerState.currentPage
                        )
                    }
                }

                item {
                    SectionHeader(onClick = onTransactionsClicked)
                }

                balances[balancePagerState.currentPage].transactions.forEach { (date, transactions) ->
                    item(key = date) {
                        DateHeader(date = date)
                    }

                    items(
                        items = transactions,
                        key = { it.id }
                    ) { transaction ->
                        Transaction(
                            transaction = transaction,
                            onClick = onClick
                        )
                    }
                }
            }
        }
            ?: NoTransactionsContent(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxWidth()
                    .padding(horizontal = HomeScreenPadding)
                    .padding(vertical = 8.dp),
                onClick = add
            )
    }
}

@Composable
private fun BalanceCard(
    balance: HomeState.Loaded.BalanceUiModel,
    onClick: () -> Unit
) {
    val containerColor = when {
        balance.expense > balance.income -> MaterialTheme.colorScheme.errorContainer
        balance.income > balance.expense -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        balance.expense > balance.income -> MaterialTheme.colorScheme.onErrorContainer
        balance.income > balance.expense -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = HomeCardShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HomeCardPadding)
        ) {
            Text(
                text = balance.yearMonth.toMonthName(),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = balance.balance.toHumanReadableValue(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            Text(
                text = stringResource(Res.string.balance),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(HomeCardPadding))

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SummaryItem(
                    label = stringResource(Res.string.income_total),
                    value = balance.income,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    contentColor = contentColor
                )

                SummaryItem(
                    label = stringResource(Res.string.expense_total),
                    value = balance.expense,
                    indicatorColor = MaterialTheme.colorScheme.error,
                    contentColor = contentColor
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: Double,
    indicatorColor: Color,
    contentColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )

            Spacer(Modifier.width(6.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.7f)
            )
        }

        Text(
            text = value.toHumanReadableValue(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

@Composable
private fun PageIndicator(
    count: Int,
    currentPage: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            space = 6.dp,
            alignment = Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val isCurrent = index == currentPage

            Box(
                modifier = Modifier
                    .size(if (isCurrent) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    )
            )
        }
    }
}

@Composable
private fun SectionHeader(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.latest_transactions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(
                text = stringResource(Res.string.see_all),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun DateHeader(date: LocalDate) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp),
        text = date.format(
            format = LocalDate.Format {
                day()
                char('/')
                monthNumber()
                char('/')
                year()
            }
        ),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun Transaction(
    transaction: TransactionDomainModel,
    onClick: (Long) -> Unit
) {
    val isExpense = transaction.type == TransactionType.Expense
    val amount = transaction.amount.toHumanReadableValue()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(transaction.id) },
        shape = HomeCardShape,
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

            val category = transaction.category.getValueForComposableUI()
            val description = transaction.description?.takeUnless { it.isBlank() }

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
                text = if (isExpense) "-$amount" else "+$amount",
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
