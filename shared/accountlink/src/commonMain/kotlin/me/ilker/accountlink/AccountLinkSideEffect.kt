package me.ilker.accountlink

import me.ilker.core.SideEffect

sealed interface AccountLinkSideEffect : SideEffect {
    data object LinkComplete : AccountLinkSideEffect
}
