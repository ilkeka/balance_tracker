package me.ilker.auth

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.ilker.balance_tracker.auth.authApi
import me.ilker.core.Manager

class RegistrationManager : Manager<RegistrationState, RegistrationIntent, RegistrationSideEffect>() {
    private val managerState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    override val state: StateFlow<RegistrationState> = managerState.asStateFlow()
    override val sideEffect: Channel<RegistrationSideEffect> = Channel(capacity = 1)

    override fun sendIntent(intent: RegistrationIntent) {
        when (intent) {
            is RegistrationIntent.Register -> register(intent.email, intent.password)
            is RegistrationIntent.Reset -> managerState.value = RegistrationState.Idle
        }
    }

    private fun register(email: String, password: String) {
        if (managerState.value is RegistrationState.Loading) return

        managerState.value = RegistrationState.Loading(email, password)
        scope.launch {
            runCatching {
                authApi.authenticate(email, password)
            }.onSuccess {
                managerState.value = RegistrationState.Success("Authenticated")
                sideEffect.trySend(RegistrationSideEffect.RegistrationComplete)
            }.onFailure { e ->
                managerState.value = RegistrationState.Error(
                    e.message ?: "Authentication failed"
                )
            }
        }
    }
}
