package me.ilker.auth

import me.ilker.core.State

sealed interface RegistrationState : State {
    data object Idle : RegistrationState
    data class Loading(val email: String, val password: String) : RegistrationState
    data class Success(val message: String) : RegistrationState
    data class Error(val result: AuthenticationResult) : RegistrationState
}
