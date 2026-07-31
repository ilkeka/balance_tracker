package me.ilker.accountlink

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.core.Manager

class AccountLinkManager(
    private val sdk: BalanceTrackerSDK
) : Manager<AccountLinkState, AccountLinkIntent, AccountLinkSideEffect>() {
    private val managerState = MutableStateFlow<AccountLinkState>(AccountLinkState.Loading)
    override val state: StateFlow<AccountLinkState> = managerState.asStateFlow()
    override val sideEffect: Channel<AccountLinkSideEffect> = Channel(capacity = 1)

    private var lastToken: String? = null

    init {
        refreshToken()
    }

    override fun sendIntent(intent: AccountLinkIntent) {
        when (intent) {
            is AccountLinkIntent.RefreshToken -> refreshToken()
            is AccountLinkIntent.Link -> link(intent.token)
            is AccountLinkIntent.DismissMessage -> {
                lastToken?.let { managerState.value = AccountLinkState.Idle(it) }
            }
        }
    }

    private fun refreshToken() {
        if (managerState.value is AccountLinkState.Loading) return

        managerState.value = AccountLinkState.Loading
        scope.launch {
            runCatching {
                sdk.getLinkToken()
            }.onSuccess { token ->
                lastToken = token
                managerState.value = AccountLinkState.Idle(token)
            }.onFailure {
                managerState.value = AccountLinkState.Error(AccountLinkError.Failed)
            }
        }
    }

    private fun link(token: String) {
        if (managerState.value is AccountLinkState.Linking) return

        managerState.value = AccountLinkState.Linking
        scope.launch {
            runCatching {
                sdk.linkAccount(token)
            }.onSuccess {
                managerState.value = AccountLinkState.Linked
                sideEffect.trySend(AccountLinkSideEffect.LinkComplete)
            }.onFailure {
                managerState.value = AccountLinkState.Error(AccountLinkError.Failed)
            }
        }
    }
}
