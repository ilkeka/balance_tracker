package me.ilker.profile

import me.ilker.balance_tracker.sdk.LinkedAccount
import me.ilker.core.State

enum class ProfileError {
    Failed
}

sealed interface ProfileState : State {
    data object Loading : ProfileState
    data class Idle(val token: String) : ProfileState
    data object Linking : ProfileState
    data class Linked(val linkedAccount: LinkedAccount) : ProfileState
    data object LoggingOut : ProfileState
    data class Error(val result: ProfileError) : ProfileState
}
