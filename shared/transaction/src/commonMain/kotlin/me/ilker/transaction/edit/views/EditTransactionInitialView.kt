package me.ilker.transaction.edit.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.edit_transaction
import me.ilker.transaction.common.CardSpacing
import me.ilker.transaction.common.ScreenPadding
import me.ilker.transaction.common.SkeletonBar
import me.ilker.transaction.common.SkeletonCard
import me.ilker.transaction.common.TransactionTopBar
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EditTransactionInitialView(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TransactionTopBar(
                title = stringResource(Res.string.edit_transaction),
                onBack = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = ScreenPadding,
                end = ScreenPadding,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(CardSpacing)
        ) {
            items(count = 5) {
                FormFieldSkeleton()
            }
        }
    }
}

@Composable
private fun FormFieldSkeleton() {
    SkeletonCard {
        SkeletonBar(height = 12.dp)
        SkeletonBar(height = 52.dp)
    }
}
