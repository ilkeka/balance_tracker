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
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.add
import me.ilker.balance_tracker.resources.amount
import me.ilker.balance_tracker.resources.app_name
import me.ilker.balance_tracker.resources.balance
import me.ilker.balance_tracker.resources.date
import me.ilker.balance_tracker.resources.description
import me.ilker.balance_tracker.resources.expense_total
import me.ilker.balance_tracker.resources.income_total
import me.ilker.balance_tracker.resources.latest_transactions
import me.ilker.balance_tracker.resources.see_all
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.home.HomeState
import org.jetbrains.compose.resources.stringResource

@ExperimentalMaterial3Api
@Composable
internal fun HomeLoadedView(
    state: HomeState.Loaded,
    add: () -> Unit,
    onTransactionsClicked: () -> Unit,
    onClick: (id: Long) -> Unit
) {
    val balancePagerState = rememberPagerState(
        initialPage = state.balances.lastIndex,
        pageCount = { state.balances.size }
    )

    val transactionsPagerState = rememberPagerState(
        initialPage = balancePagerState.currentPage,
        pageCount = { state.balances.size }
    )

    LaunchedEffect(balancePagerState) {
        snapshotFlow { balancePagerState.currentPage }.collect {
            transactionsPagerState.animateScrollToPage(it)
        }
    }

    LaunchedEffect(transactionsPagerState) {
        snapshotFlow { transactionsPagerState.currentPage }.collect {
            balancePagerState.animateScrollToPage(it)
        }
    }

    Scaffold(
        modifier = Modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 48.dp)
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.app_name),
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
                onClick = add,
                colors = ButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.33f),
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.33f),
                ),
                content = {
                    Text(stringResource(Res.string.add))
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                HorizontalPager(
                    state = balancePagerState
                ) { page ->
                    val balance = state.balances[page]
                    val summaryColor = when {
                        balance.expense > balance.income -> MaterialTheme.colorScheme.errorContainer
                        balance.expense < balance.income -> Color(0xFF34501F)
                        else -> MaterialTheme.colorScheme.surfaceContainer
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        colors = CardDefaults.cardColors(
                            contentColor = Color(0xFFD0DBD0),
                            containerColor = summaryColor,
                            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.33f),
                            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.33f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp))
                        ) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = balance.selectedDate,
                                fontSize = TextUnit(value = 18f, type = TextUnitType.Sp),
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "${stringResource(Res.string.balance)}: ${balance.balance}",
                            )

                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "${stringResource(Res.string.income_total)}: ${balance.income}",
                            )

                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = "${stringResource(Res.string.expense_total)}: ${balance.expense}",
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
                            .padding(end = 12.dp),
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
                HorizontalPager(
                    state = transactionsPagerState,
                    verticalAlignment = Alignment.Top
                ) { page ->
                    val transactions = state.balances[page].transactions


                    Column {
                        transactions.forEach { transaction ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onClick(transaction.id)
                                    }
                                    .padding(horizontal = 12.dp),
                                colors = CardDefaults.cardColors(
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    containerColor = when (transaction.type) {
                                        TransactionType.Expense -> MaterialTheme.colorScheme.errorContainer
                                        TransactionType.Income -> MaterialTheme.colorScheme.tertiaryContainer
                                    },
                                    disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.33f),
                                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.33f),
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp))
                                ) {
                                    val amountString = stringResource(Res.string.amount)
                                    val dateString = stringResource(Res.string.date)

                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = "$amountString: ${transaction.amount}",
                                    )

                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = "$dateString: ${transaction.dateTime}",
                                    )

                                    transaction.description?.takeUnless { it.isBlank() }?.let { description ->
                                        Text(
                                            modifier = Modifier.fillMaxWidth(),
                                            text = "${stringResource(Res.string.description)}: $description",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
