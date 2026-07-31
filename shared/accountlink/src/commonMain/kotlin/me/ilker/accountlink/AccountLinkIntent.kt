package me.ilker.accountlink

import me.ilker.core.Intent

sealed interface AccountLinkIntent : Intent {
    data object RefreshToken : AccountLinkIntent
    data class Link(val token: String) : AccountLinkIntent
    data object DismissMessage : AccountLinkIntent
}
