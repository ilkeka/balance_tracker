package me.ilker.transaction.transactions.views

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import me.ilker.balance_tracker.resources.date
import me.ilker.balance_tracker.resources.transaction_type
import me.ilker.transaction.transactions.TransactionDomainModel
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TransactionsLoadedView(
    transactions: List<TransactionDomainModel>,
    add: () -> Unit
) {
    Scaffold(
        modifier = Modifier.padding(vertical = 12.dp),
        topBar = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(12.dp),
                    text = stringResource(Res.string.app_name),
                    fontSize = TextUnit(value = 18f, type = TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        bottomBar = {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                onClick = add,
                colors = ButtonColors(
                    contentColor = Color(0xD0FFFFFF),
                    containerColor = Color(0xD0EC5050),
                    disabledContentColor = Color(0xD0FFFFFF),
                    disabledContainerColor = Color(0x1A884DFA),
                ),
                content = {
                    Text(stringResource(Res.string.add))
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions) { transaction ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF88CC),
                        contentColor = Color.Black,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = Color.Black
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
                    }
                }

                HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp)
            }
        }
    }
}
