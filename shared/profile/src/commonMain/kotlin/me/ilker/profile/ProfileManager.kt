package me.ilker.profile

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.core.Manager

class ProfileManager(
    private val sdk: BalanceTrackerSDK
) : Manager<ProfileState, ProfileIntent, ProfileSideEffect>() {
    private val managerState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    override val state: StateFlow<ProfileState> = managerState.asStateFlow()
    override val sideEffect: Channel<ProfileSideEffect> = Channel(capacity = 1)

    private var lastToken: String? = null

    init {
        refreshToken()
    }

    override fun sendIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.RefreshToken -> refreshToken()
            is ProfileIntent.Link -> link(intent.token)
            ProfileIntent.DismissMessage -> {
                lastToken?.let { managerState.value = ProfileState.Idle(it) }
            }
        }
    }

    private fun refreshToken() {
        if (managerState.value is ProfileState.Loading) return

        managerState.value = ProfileState.Loading
        scope.launch {
            runCatching {
                sdk.getLinkToken()
            }.onSuccess { token ->
                lastToken = token
                managerState.value = ProfileState.Idle(token)
            }.onFailure {
                managerState.value = ProfileState.Error(ProfileError.Failed)
            }
        }
    }

    private fun link(token: String) {
        if (managerState.value is ProfileState.Linking) return

        managerState.value = ProfileState.Linking
        scope.launch {
            runCatching {
                sdk.linkAccount(token)
            }.onSuccess {
                managerState.value = ProfileState.Linked
                sideEffect.trySend(ProfileSideEffect.LinkComplete)
            }.onFailure {
                managerState.value = ProfileState.Error(ProfileError.Failed)
            }
        }
    }
}
