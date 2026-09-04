package me.ilker.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import me.ilker.profile.views.ProfileView

@Composable
fun ProfileScreen(
    state: State<ProfileState>,
    email: String?,
    onRefreshToken: () -> Unit,
    onLink: (token: String) -> Unit,
    onDismissMessage: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    ProfileView(
        state = state,
        email = email,
        onRefreshToken = onRefreshToken,
        onLink = onLink,
        onDismissMessage = onDismissMessage,
        onLogout = onLogout,
        onBack = onBack
    )
}
