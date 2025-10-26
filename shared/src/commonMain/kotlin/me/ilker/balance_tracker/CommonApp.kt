package me.ilker.balance_tracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.ilker.core.Route
import me.ilker.transaction.add.AddTransactionScreen

@Composable
fun CommonApp() {
    MaterialTheme {
        val navController: NavHostController = rememberNavController()

        NavHost(navController = navController, startDestination = Route.Root) {
            composable<Route.Root> {
                RootScreen()
            }

            composable<Route.Add> {
                AddTransactionScreen()
            }
        }
    }
}
