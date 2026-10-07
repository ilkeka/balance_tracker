package me.ilker.transaction.details.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.transaction_details
import me.ilker.transaction.common.CardSpacing
import me.ilker.transaction.common.ScreenPadding
import me.ilker.transaction.common.SkeletonBar
import me.ilker.transaction.common.SkeletonCard
import me.ilker.transaction.common.SkeletonPlaceholder
import me.ilker.transaction.common.TransactionTopBar
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TransactionDetailsInitialView(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TransactionTopBar(
                title = stringResource(Res.string.transaction_details),
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
            item { AmountSkeleton() }

            item { DetailsSkeleton() }

            item {
                SkeletonBar(height = 48.dp, shape = RoundedCornerShape(50))
            }
        }
    }
}

@Composable
private fun AmountSkeleton() {
    SkeletonCard(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SkeletonPlaceholder(width = 160.dp, height = 40.dp)
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SkeletonPlaceholder(width = 90.dp, height = 28.dp, shape = RoundedCornerShape(50))
        }
    }
}

@Composable
private fun DetailsSkeleton() {
    SkeletonCard {
        SkeletonBar(height = 12.dp)
        SkeletonBar(height = 44.dp)
        SkeletonBar(height = 12.dp)
        SkeletonBar(height = 44.dp)
        SkeletonBar(height = 12.dp)
        SkeletonBar(height = 44.dp)
    }
}
