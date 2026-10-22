package me.ilker.transaction.transactions.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.transactions
import me.ilker.transaction.common.CardSpacing
import me.ilker.transaction.common.ScreenPadding
import me.ilker.transaction.common.SkeletonCard
import me.ilker.transaction.common.SkeletonPlaceholder
import me.ilker.transaction.common.TransactionTopBar
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TransactionsInitialView(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TransactionTopBar(
                title = stringResource(Res.string.transactions),
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
            item { BalanceSkeleton() }

            item { SectionHeaderSkeleton() }

            items(count = 3) {
                TransactionSkeleton()
            }
        }
    }
}

@Composable
private fun BalanceSkeleton() {
    SkeletonCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        SkeletonPlaceholder(width = 80.dp, height = 14.dp)
        SkeletonPlaceholder(width = 160.dp, height = 36.dp)
        SkeletonPlaceholder(width = 220.dp, height = 20.dp)
    }
}

@Composable
private fun SectionHeaderSkeleton() {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonPlaceholder(width = 150.dp, height = 18.dp)

        Spacer(Modifier.weight(1f))

        SkeletonPlaceholder(width = 72.dp, height = 32.dp, shape = CircleShape)
    }
}

@Composable
private fun TransactionSkeleton() {
    SkeletonCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonPlaceholder(width = 44.dp, height = 44.dp, shape = CircleShape)

            Spacer(Modifier.padding(start = 16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonPlaceholder(width = 120.dp, height = 14.dp)
                SkeletonPlaceholder(width = 80.dp, height = 12.dp)
            }
        }
    }
}
