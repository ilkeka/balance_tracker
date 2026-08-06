package me.ilker.profile

import me.ilker.core.State

enum class ProfileError {
    Failed
}

sealed interface ProfileState : State {
    data object Loading : ProfileState
    data class Idle(val token: String) : ProfileState
    data object Linking : ProfileState
    data object Linked : ProfileState
    data class Error(val result: ProfileError) : ProfileState
}
