package me.ilker.auth

import me.ilker.core.Intent

sealed interface RegistrationIntent : Intent {
    data class Register(val email: String, val password: String) : RegistrationIntent
    data object Reset : RegistrationIntent
}
