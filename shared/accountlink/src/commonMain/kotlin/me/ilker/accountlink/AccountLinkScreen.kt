package me.ilker.accountlink

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import me.ilker.accountlink.views.AccountLinkView

@Composable
fun AccountLinkScreen(
    state: State<AccountLinkState>,
    onRefreshToken: () -> Unit,
    onLink: (token: String) -> Unit,
    onDismissMessage: () -> Unit,
    onBack: () -> Unit
) {
    AccountLinkView(
        state = state,
        onRefreshToken = onRefreshToken,
        onLink = onLink,
        onDismissMessage = onDismissMessage,
        onBack = onBack
    )
}
