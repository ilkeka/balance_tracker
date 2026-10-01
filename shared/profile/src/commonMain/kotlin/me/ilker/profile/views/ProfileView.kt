package me.ilker.profile.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.account_link_copied
import me.ilker.balance_tracker.resources.account_link_copy_code
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
import me.ilker.balance_tracker.resources.email
import me.ilker.balance_tracker.resources.linked_account
import me.ilker.balance_tracker.resources.logout
import me.ilker.balance_tracker.resources.not_signed_in
import me.ilker.balance_tracker.resources.profile
import me.ilker.balance_tracker.sdk.LinkedAccount
import me.ilker.profile.clipboard.rememberCopyToClipboard
import me.ilker.profile.ProfileState
import me.ilker.profile.scanner.ManualTokenEntry
import me.ilker.profile.scanner.QrScanner
import org.jetbrains.compose.resources.stringResource

private val CardShape = RoundedCornerShape(24.dp)
private val CardPadding = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileView(
    state: State<ProfileState>,
    email: String?,
    linkAttempted: Boolean,
    onRefreshToken: () -> Unit,
    onLink: (token: String) -> Unit,
    onDismissMessage: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val currentState = state.value
    val linkedAccount = (currentState as? ProfileState.Linked)?.linkedAccount
    val loggingOut = currentState is ProfileState.LoggingOut
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentState) {
        if (currentState is ProfileState.Linking) selectedTab = 1
    }

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ProfileTopBar(onBack = onBack) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CardPadding)
                .padding(bottom = CardPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AccountCard(email = email, linkedAccount = linkedAccount)

            when (currentState) {
                ProfileState.Loading -> LoadingCard()
                is ProfileState.Error -> if (linkAttempted) {
                    MessageBanner(
                        text = stringResource(Res.string.account_link_failed),
                        onDismiss = onDismissMessage
                    )
                }
                is ProfileState.Idle,
                ProfileState.Linking -> AccountLinkSection(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    linking = currentState is ProfileState.Linking,
                    token = (currentState as? ProfileState.Idle)?.token,
                    onRefreshToken = onRefreshToken,
                    onLink = onLink
                )
                is ProfileState.Linked -> LinkSuccessCard()
                ProfileState.LoggingOut -> Unit
            }

            LogoutButton(
                loggingOut = loggingOut,
                onLogout = onLogout
            )
        }
    }
}

@Composable
private fun ProfileTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp)
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
            text = stringResource(Res.string.profile),
            fontSize = TextUnit(value = 24f, type = TextUnitType.Sp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AccountCard(
    email: String?,
    linkedAccount: LinkedAccount?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val initials = email?.let(::initialsOf)

                    if (initials != null) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = email ?: stringResource(Res.string.not_signed_in),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = linkedAccount?.let {
                        "${stringResource(Res.string.linked_account)}: ${it.accountId}"
                    } ?: stringResource(Res.string.email),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun AccountLinkSection(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    linking: Boolean,
    token: String?,
    onRefreshToken: () -> Unit,
    onLink: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(Res.string.account_linking),
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        enabled = !linking,
                        onClick = { onTabSelected(0) },
                        text = { Text(stringResource(Res.string.account_link_my_qr_tab)) },
                        icon = { Icon(Icons.Rounded.QrCode2, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        enabled = !linking,
                        onClick = { onTabSelected(1) },
                        text = { Text(stringResource(Res.string.account_link_scan_tab)) },
                        icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) }
                    )
                }

                when (selectedTab) {
                    0 if token != null -> MyQrContent(
                        token = token,
                        onRefresh = onRefreshToken
                    )
                    0 -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    else -> ScanContent(
                        linking = linking,
                        onLink = onLink
                    )
                }
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
    val copyToClipboard = rememberCopyToClipboard()
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardPadding, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Image(
                painter = qrPainter,
                contentDescription = stringResource(Res.string.account_link_qr_description),
                modifier = Modifier
                    .padding(20.dp)
                    .size(220.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(Res.string.account_link_scan_hint),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        Surface(
            modifier = Modifier.clickable {
                copyToClipboard(token)
                copied = true
            },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = stringResource(Res.string.account_link_copy_code),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.width(12.dp))

                Text(
                    text = token,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        CopiedLabel(
            visible = copied,
            text = stringResource(Res.string.account_link_copied)
        )

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onRefresh,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(8.dp))

            Text(stringResource(Res.string.account_link_refresh))
        }
    }
}

@Composable
private fun CopiedLabel(
    visible: Boolean,
    text: String
) {
    if (visible) {
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
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
            .fillMaxWidth()
            .padding(horizontal = CardPadding, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            QrScanner(onScanned = onLink)
        }

        Text(
            text = stringResource(Res.string.account_link_manual_hint),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ManualTokenEntry(
            enabled = !linking,
            onScanned = onLink
        )

        if (linking) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun LinkSuccessCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = stringResource(Res.string.account_link_success),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MessageBanner(
    text: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = CardPadding, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(stringResource(Res.string.account_link_done))
            }
        }
    }
}

@Composable
private fun LogoutButton(
    loggingOut: Boolean,
    onLogout: () -> Unit
) {
    OutlinedButton(
        onClick = onLogout,
        enabled = !loggingOut,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
            disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.33f),
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        if (loggingOut) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.width(8.dp))
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = stringResource(Res.string.logout),
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(8.dp))
        }

        Text(stringResource(Res.string.logout))
    }
}

private fun initialsOf(email: String): String? {
    val localPart = email.substringBefore("@").trim()

    if (localPart.isEmpty()) return null

    return localPart
        .split('.', '_', '-')
        .mapNotNull { part -> part.trim().firstOrNull() }
        .take(2)
        .joinToString(separator = "") { it.uppercaseChar().toString() }
        .takeIf { it.isNotEmpty() }
}