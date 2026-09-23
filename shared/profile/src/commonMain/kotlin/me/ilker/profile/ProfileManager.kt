package me.ilker.profile

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.LinkedAccount
import me.ilker.core.Manager

class ProfileManager(
    private val sdk: BalanceTrackerSDK
) : Manager<ProfileState, ProfileIntent, ProfileSideEffect>() {
    private val managerState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    override val state: StateFlow<ProfileState> = managerState.asStateFlow()
    override val sideEffect: Channel<ProfileSideEffect> = Channel(capacity = 1)

    private var lastToken: String? = null
    private val _linkAttempted = MutableStateFlow(false)
    val linkAttempted: StateFlow<Boolean> = _linkAttempted.asStateFlow()

    init {
        refreshToken()
    }

    override fun sendIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.RefreshToken -> refreshToken()
            is ProfileIntent.Link -> link(intent.token)
            ProfileIntent.DismissMessage -> {
                _linkAttempted.value = false
                val cachedToken: String? = lastToken
                if (cachedToken != null && managerState.value is ProfileState.Error) {
                    managerState.value = ProfileState.Idle(cachedToken)
                }
            }
            ProfileIntent.Logout -> logout()
        }
    }

    private fun refreshToken() {
        val current = managerState.value
        if (current is ProfileState.Loading && lastToken != null) return
        if (current is ProfileState.Linking || current is ProfileState.LoggingOut) return

        managerState.value = ProfileState.Loading
        scope.launch {
            runCatching {
                loadState()
            }.onSuccess { state ->
                if (state is ProfileState.Idle) lastToken = state.token
                managerState.value = state
            }.onFailure {
                managerState.value = ProfileState.Error(ProfileError.Failed)
            }
        }
    }

    private suspend fun loadState(): ProfileState {
        val linkedAccount = sdk.getLinkedAccount()
        if (linkedAccount != null) return ProfileState.Linked(linkedAccount)
        return ProfileState.Idle(sdk.getLinkToken())
    }

    private fun link(token: String) {
        if (managerState.value is ProfileState.Linking) return

        _linkAttempted.value = true
        managerState.value = ProfileState.Linking
        scope.launch {
            runCatching {
                sdk.linkAccount(token)
            }.onSuccess { linkedAccount: LinkedAccount ->
                managerState.value = ProfileState.Linked(linkedAccount)
                sideEffect.trySend(ProfileSideEffect.LinkComplete)
            }.onFailure {
                managerState.value = ProfileState.Error(ProfileError.Failed)
            }
        }
    }

    private fun logout() {
        if (managerState.value is ProfileState.LoggingOut) return

        managerState.value = ProfileState.LoggingOut
        scope.launch {
            runCatching {
                sdk.logout()
            }.onSuccess {
                sideEffect.trySend(ProfileSideEffect.LogoutComplete)
            }.onFailure {
                managerState.value = ProfileState.Error(ProfileError.Failed)
            }
        }
    }
}
