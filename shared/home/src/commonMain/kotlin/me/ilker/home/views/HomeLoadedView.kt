package me.ilker.home.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.account_linking
import me.ilker.balance_tracker.resources.add
import me.ilker.balance_tracker.resources.amount
import me.ilker.balance_tracker.resources.app_name
import me.ilker.balance_tracker.resources.authenticate
import me.ilker.balance_tracker.resources.balance
import me.ilker.balance_tracker.resources.category
import me.ilker.balance_tracker.resources.description
import me.ilker.balance_tracker.resources.expense_total
import me.ilker.balance_tracker.resources.income_total
import me.ilker.balance_tracker.resources.latest_transactions
import me.ilker.balance_tracker.resources.month_names
import me.ilker.balance_tracker.resources.see_all
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sdk.getValueForComposableUI
import me.ilker.core.extensions.toHumanReadableValue
import me.ilker.home.HomeState
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@ExperimentalMaterial3Api
@Composable
internal fun HomeLoadedView(
    state: HomeState.Loaded,
    user: HomeState.User,
    setSelectedYearMonth: (yearMonth: YearMonth) -> Unit,
    add: () -> Unit,
    onTransactionsClicked: () -> Unit,
    onClick: (id: Long) -> Unit,
    onRegister: () -> Unit = {},
    onAccountLink: () -> Unit = {}
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
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 48.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.app_name),
                    fontSize = TextUnit(value = 24f, type = TextUnitType.Sp),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                user.sessionEmail?.let { sessionEmail ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = sessionEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(onClick = onAccountLink) {
                            Icon(
                                imageVector = Icons.Rounded.QrCode2,
                                contentDescription = stringResource(Res.string.account_linking)
                            )
                        }
                    }
                } ?: run {
                    Button(
                        onClick = onRegister,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(Res.string.authenticate))
                    }
                }
            }
        },
        bottomBar = {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                onClick = add,
                colors = ButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.33f),
                    disabledContainerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.33f),
                ),
                content = {
                    Text(stringResource(Res.string.add))
                }
            )
        }
    ) { paddingValues ->
        state.balances.takeUnless { it.isEmpty() }?.let {
            LazyColumn(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    HorizontalPager(
                        modifier = Modifier.fillMaxWidth(),
                        state = balancePagerState,
                        verticalAlignment = Alignment.Top,
                        pageSize = PageSize.Fill,
                        contentPadding = PaddingValues(
                            start = if(balancePagerState.currentPage == 0) {
                                0.dp
                            } else {
                                16.dp
                            }
                        )
                    ) { page ->
                        val balance = state.balances[page]
                        val summaryContentColor = when {
                            balance.expense > balance.income -> MaterialTheme.colorScheme.error
                            balance.expense < balance.income -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.surfaceContainer
                        }
                        val summaryContainerColor = when {
                            balance.expense > balance.income -> MaterialTheme.colorScheme.errorContainer
                            balance.expense < balance.income -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainer
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTransactionsClicked()
                                }
                                .padding(horizontal = 12.dp),
                            colors = CardDefaults.cardColors(
                                contentColor = summaryContentColor,
                                containerColor = summaryContainerColor,
                                disabledContentColor = summaryContentColor.copy(alpha = 0.33f),
                                disabledContainerColor = summaryContainerColor.copy(alpha = 0.33f),
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp))
                            ) {
                                val monthNames = stringArrayResource(Res.array.month_names)

                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = balance.yearMonth.format(
                                        YearMonth.Format {
                                            monthName(names = MonthNames(monthNames))
                                            char(' ')
                                            year()
                                        }
                                    ),
                                    fontSize = TextUnit(value = 18f, type = TextUnitType.Sp),
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = with(balance.balance) {
                                        "${stringResource(Res.string.balance)}: ${this.toHumanReadableValue()}"
                                    },
                                    fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = with(balance.income) {
                                        "${stringResource(Res.string.income_total)}: ${this.toHumanReadableValue()}"
                                    },
                                    fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = with(balance.expense) {
                                        "${stringResource(Res.string.expense_total)}: ${this.toHumanReadableValue()}"
                                    },
                                    fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                                .clickable {
                                    onTransactionsClicked()
                                },
                            text = stringResource(Res.string.latest_transactions),
                            fontSize = TextUnit(value = 18f, type = TextUnitType.Sp),
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            modifier = Modifier
                                .clickable {
                                    onTransactionsClicked()
                                },
                            text = stringResource(Res.string.see_all),
                            fontSize = TextUnit(value = 12f, type = TextUnitType.Sp),
                            fontWeight = FontWeight.Light
                        )
                    }
                }

                item {
                    val transactionsByLocalDate = state.balances[balancePagerState.currentPage].transactions

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        transactionsByLocalDate.forEach { transactions ->
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = transactions.key.format(
                                    format = LocalDate.Format {
                                        day()
                                        char('/')
                                        monthNumber()
                                        char('/')
                                        year()
                                    }
                                ),
                                fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                            )

                            transactions.value.forEach { transaction ->
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Transaction(
                                        transaction = transaction,
                                        onClick = onClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
            ?: NoTransactionsContent(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxWidth()
                    .clickable { add() }
                    .padding(horizontal = 12.dp)
            )
    }
}

@Composable
private fun Transaction(
    transaction: TransactionDomainModel,
    onClick: (Long) -> Unit
) {
    val contentColor = when(transaction.type) {
        TransactionType.Expense -> MaterialTheme.colorScheme.onError
        TransactionType.Income -> MaterialTheme.colorScheme.onPrimary
    }
    val containerColor = when(transaction.type) {
        TransactionType.Expense -> MaterialTheme.colorScheme.onErrorContainer
        TransactionType.Income -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick(transaction.id)
            },
        colors = CardDefaults.cardColors(
            contentColor = contentColor,
            containerColor = containerColor,
            disabledContentColor = contentColor.copy(alpha = 0.33f),
            disabledContainerColor = containerColor.copy(alpha = 0.33f),
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp))
        ) {
            val amountString = stringResource(Res.string.amount)
            val categoryString = stringResource(Res.string.category)

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "$amountString: ${transaction.amount.toHumanReadableValue()}",
                fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "$categoryString: ${transaction.category.getValueForComposableUI()}",
                fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
            )

            transaction.description?.takeUnless { it.isBlank() }?.let { description ->
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "${stringResource(Res.string.description)}: $description",
                    fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                )
            }
        }
    }
}
