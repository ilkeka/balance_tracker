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
import me.ilker.balance_tracker.resources.date
import me.ilker.balance_tracker.resources.description
import me.ilker.balance_tracker.resources.nothing_yet
import me.ilker.balance_tracker.resources.start_create_transaction
import me.ilker.balance_tracker.resources.transactions
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.theme.SurfaceColor
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
                    items(state.transactions) { transaction ->
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
