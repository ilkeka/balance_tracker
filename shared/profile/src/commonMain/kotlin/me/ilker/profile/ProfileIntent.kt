package me.ilker.profile

import me.ilker.core.Intent

sealed interface ProfileIntent : Intent {
    data object RefreshToken : ProfileIntent
    data class Link(val token: String) : ProfileIntent
    data object DismissMessage : ProfileIntent
    data object Logout : ProfileIntent
}
