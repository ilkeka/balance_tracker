package me.ilker.auth

import me.ilker.core.SideEffect

sealed interface RegistrationSideEffect : SideEffect {
    data object RegistrationComplete : RegistrationSideEffect
}
