package me.ilker.transaction.transactions.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.ilker.transaction.transactions.TransactionDomainModel

@Composable
internal fun TransactionsLoadedView(
    transactions: List<TransactionDomainModel>,
    add: () -> Unit
) {
    Scaffold(
        modifier = Modifier,
        topBar = {

        },
        bottomBar = {
            IconButton(
                onClick = add,
                content = {

                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .safeContentPadding()
                .background(MaterialTheme.colorScheme.onError)
        ) {
            items(transactions) { transaction ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {}
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = transaction.amount.toString()
                    )
                }

            }
        }
    }
}
