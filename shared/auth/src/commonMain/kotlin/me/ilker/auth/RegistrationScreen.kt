package me.ilker.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import me.ilker.auth.views.AuthenticationView

@Composable
fun RegistrationScreen(
    state: State<RegistrationState>,
    onRegister: (email: String, password: String) -> Unit,
    onBack: () -> Unit
) {
    AuthenticationView(
        state = state,
        onRegister = onRegister,
        onBack = onBack
    )
}
