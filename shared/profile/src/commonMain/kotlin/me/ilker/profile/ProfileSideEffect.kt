package me.ilker.profile

import me.ilker.core.SideEffect

sealed interface ProfileSideEffect : SideEffect {
    data object LinkComplete : ProfileSideEffect
    data object LogoutComplete : ProfileSideEffect
}
