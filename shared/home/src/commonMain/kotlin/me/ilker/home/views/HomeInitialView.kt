package me.ilker.home.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.ilker.home.HomeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeInitialView(
    device: HomeState.Device,
    onSync: () -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeTopBar(
                device = device,
                onSync = onSync
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = HomeScreenPadding,
                end = HomeScreenPadding,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = HomeCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HomeCardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonPlaceholder(width = 80.dp, height = 14.dp)
            SkeletonPlaceholder(width = 160.dp, height = 36.dp)
            SkeletonPlaceholder(width = 220.dp, height = 20.dp)
        }
    }
}

@Composable
private fun SectionHeaderSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonPlaceholder(width = 150.dp, height = 18.dp)

        Spacer(Modifier.weight(1f))

        SkeletonPlaceholder(width = 72.dp, height = 32.dp)
    }
}

@Composable
private fun TransactionSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = HomeCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonPlaceholder(width = 44.dp, height = 44.dp, shape = CircleShape)

            Spacer(Modifier.width(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonPlaceholder(width = 120.dp, height = 14.dp)
                SkeletonPlaceholder(width = 80.dp, height = 12.dp)
            }
        }
    }
}

@Composable
private fun SkeletonPlaceholder(
    width: Dp,
    height: Dp,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    )
}
