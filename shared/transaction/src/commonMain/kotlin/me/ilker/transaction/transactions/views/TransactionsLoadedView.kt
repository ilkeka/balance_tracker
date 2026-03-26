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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import me.ilker.balance_tracker.resources.description
import me.ilker.balance_tracker.resources.nothing_yet
import me.ilker.balance_tracker.resources.start_create_transaction
import me.ilker.balance_tracker.resources.transactions
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sdk.getValueForComposableUI
import me.ilker.transaction.transactions.TransactionState
import org.jetbrains.compose.resources.stringResource

@ExperimentalMaterial3Api
@Composable
internal fun TransactionsLoadedView(
    state: TransactionState.Loaded,
    onClick: (id: Long) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier,
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
                    text = stringResource(Res.string.transactions),
                    fontSize = TextUnit(value = 24f, type = TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        state.transactions
            .takeUnless { it.isEmpty() }
            ?.let {
                LazyColumn(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.transactions.forEach { transactionsByLocalDate ->
                        item(transactionsByLocalDate.key) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp)),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = transactionsByLocalDate.key.toString(),
                                    fontSize = TextUnit(value = 16f, type = TextUnitType.Sp),
                                )

                                transactionsByLocalDate.value.forEach { transaction ->
                                    val contentColor = when (transaction.type) {
                                        TransactionType.Expense -> MaterialTheme.colorScheme.onError
                                        TransactionType.Income -> MaterialTheme.colorScheme.onPrimary
                                    }
                                    val containerColor = when (transaction.type) {
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
                                                text = "$amountString: ${transaction.amount}",
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
    }
}
