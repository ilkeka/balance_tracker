package me.ilker.accountlink

import me.ilker.core.State

enum class AccountLinkError {
    Failed
}

sealed interface AccountLinkState : State {
    data object Loading : AccountLinkState
    data class Idle(val token: String) : AccountLinkState
    data object Linking : AccountLinkState
    data object Linked : AccountLinkState
    data class Error(val result: AccountLinkError) : AccountLinkState
}
