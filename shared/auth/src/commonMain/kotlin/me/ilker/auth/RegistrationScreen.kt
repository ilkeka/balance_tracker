package me.ilker.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import me.ilker.auth.views.RegistrationView

@Composable
fun RegistrationScreen(
    state: State<RegistrationState>,
    onRegister: (email: String, password: String) -> Unit,
    onBack: () -> Unit
) {
    RegistrationView(
        state = state,
        onRegister = onRegister,
        onBack = onBack
    )
}
