package me.ilker.accountlink.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import me.ilker.accountlink.AccountLinkState
import me.ilker.accountlink.scanner.ManualTokenEntry
import me.ilker.accountlink.scanner.QrScanner
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.account_link_done
import me.ilker.balance_tracker.resources.account_link_failed
import me.ilker.balance_tracker.resources.account_link_manual_hint
import me.ilker.balance_tracker.resources.account_link_my_qr_tab
import me.ilker.balance_tracker.resources.account_link_qr_description
import me.ilker.balance_tracker.resources.account_link_refresh
import me.ilker.balance_tracker.resources.account_link_scan_hint
import me.ilker.balance_tracker.resources.account_link_scan_tab
import me.ilker.balance_tracker.resources.account_link_success
import me.ilker.balance_tracker.resources.account_linking
import me.ilker.balance_tracker.resources.back
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountLinkView(
    state: State<AccountLinkState>,
    onRefreshToken: () -> Unit,
    onLink: (token: String) -> Unit,
    onDismissMessage: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .padding(top = 48.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(Res.string.back)
                    )
                }
                Text(
                    text = stringResource(Res.string.account_linking),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(Res.string.account_link_my_qr_tab)) },
                    icon = { Icon(Icons.Rounded.QrCode2, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(Res.string.account_link_scan_tab)) },
                    icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) }
                )
            }

            when (val currentState = state.value) {
                is AccountLinkState.Linked -> MessageBanner(
                    text = stringResource(Res.string.account_link_success),
                    onDismiss = onDismissMessage
                )
                is AccountLinkState.Error -> MessageBanner(
                    text = stringResource(Res.string.account_link_failed),
                    onDismiss = onDismissMessage
                )
                else -> {}
            }

            when (val currentState = state.value) {
                AccountLinkState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                is AccountLinkState.Idle -> when (selectedTab) {
                    0 -> MyQrContent(
                        token = currentState.token,
                        onRefresh = onRefreshToken
                    )
                    else -> ScanContent(
                        linking = false,
                        onLink = onLink
                    )
                }
                AccountLinkState.Linking -> ScanContent(
                    linking = true,
                    onLink = onLink
                )
                else -> {}
            }
        }
    }
}

@Composable
private fun MyQrContent(
    token: String,
    onRefresh: () -> Unit
) {
    val qrPainter = rememberQrCodePainter(token)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Image(
                painter = qrPainter,
                contentDescription = stringResource(Res.string.account_link_qr_description),
                modifier = Modifier
                    .padding(16.dp)
                    .size(240.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.account_link_scan_hint),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = token,
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        OutlinedButton(onClick = onRefresh) {
            Text(stringResource(Res.string.account_link_refresh))
        }
    }
}

@Composable
private fun ScanContent(
    linking: Boolean,
    onLink: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            QrScanner(onScanned = onLink)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.account_link_manual_hint),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(12.dp))

        ManualTokenEntry(
            enabled = !linking,
            onScanned = onLink
        )

        if (linking) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun MessageBanner(
    text: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = onDismiss) {
            Text(stringResource(Res.string.account_link_done))
        }
    }
}
