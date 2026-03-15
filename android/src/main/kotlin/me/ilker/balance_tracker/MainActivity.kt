package me.ilker.balance_tracker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import me.ilker.balance_tracker.theme.BackgroundColor
import me.ilker.balance_tracker.theme.BackgroundDarkColor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = BackgroundColor.toArgb(),
                darkScrim = BackgroundDarkColor.toArgb()
            )
        )

        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            CommonApp()
        }
    }
}
