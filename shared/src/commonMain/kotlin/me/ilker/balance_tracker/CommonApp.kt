package me.ilker.balance_tracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable

@Composable
fun App() {
    MaterialTheme {
        val navController: NavHostController = rememberNavController()

        NavHost(navController = navController, startDestination = Root) {
            composable<Root> {
                RootScreen()
            }
        }
    }
}

@Serializable
object Root