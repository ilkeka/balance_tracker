package me.ilker.transaction.transactions.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import me.ilker.balance_tracker.resources.delete
import me.ilker.balance_tracker.resources.description
import me.ilker.balance_tracker.resources.edit
import me.ilker.balance_tracker.resources.expense_total
import me.ilker.balance_tracker.resources.income_total
import me.ilker.balance_tracker.resources.nothing_yet
import me.ilker.balance_tracker.resources.start_create_transaction
import me.ilker.balance_tracker.resources.transaction_type
import me.ilker.transaction.add.views.round
import me.ilker.transaction.transactions.ModalBottomSheetState
import me.ilker.transaction.transactions.TransactionDomainModel
import me.ilker.transaction.transactions.TransactionType
import org.jetbrains.compose.resources.stringResource

@ExperimentalMaterial3Api
@Composable
internal fun TransactionsLoadedView(
    transactions: List<TransactionDomainModel>,
    modalState: ModalBottomSheetState?,
    add: () -> Unit,
    onDeleteTransactions: () -> Unit,
    onDismissRequest: () -> Unit,
    onClick: (id: Long) -> Unit
) {
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
        transactions
            .takeUnless { it.isEmpty() }
            ?.let {
                LazyColumn(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    stickyHeader {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 12.dp),
                            colors = CardDefaults.cardColors(
                                contentColor = MaterialTheme.colorScheme.tertiary,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                disabledContentColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.33f),
                                disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.33f),
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp))
                            ) {
                                val (expense, income) = with(transactions.partition { it.type == TransactionType.Expense }) {
                                    this.first.sumOf { transaction -> transaction.amount }.round(2) to
                                    this.second.sumOf { transaction -> transaction.amount }.round(2)
                                }

                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "${stringResource(Res.string.balance)}: ${income - expense}",
                                )

                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "${stringResource(Res.string.income_total)}: $income",
                                )

                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "${stringResource(Res.string.expense_total)}: $expense",
                                )
                            }
                        }
                    }

                    items(transactions) { transaction ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onClick(transaction.id)
                                }
                                .padding(horizontal = 12.dp)
                            ,
                            colors = CardDefaults.cardColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
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
                                val transactionType = stringResource(Res.string.transaction_type)

                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "$amountString: ${transaction.amount}",
                                )

                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "$dateString: ${transaction.dateTime}",
                                )

                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "$transactionType: ${transaction.type.name}",
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
            ?: Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.nothing_yet),
                        fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.start_create_transaction),
                        fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                    )
                }
            }

        modalState?.let {
            ModalBottomSheet(
                onDismissRequest = onDismissRequest
            ) {
                when (modalState) {
                    is ModalBottomSheetState.ShowOptions -> ShowOptions(
                        onDeleteTransactions = onDeleteTransactions
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowOptions(
    onDeleteTransactions: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            text = stringResource(Res.string.edit),
            fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
        )

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.secondary
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onDeleteTransactions()
                }
                .padding(horizontal = 24.dp),
            text = stringResource(Res.string.delete),
            fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
        )
    }
}
