package me.ilker.balance_tracker

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import me.ilker.balance_tracker.managers.AddTransactionManager
import me.ilker.balance_tracker.managers.TransactionManager
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.core.Route
import me.ilker.transaction.add.AddTransactionIntent
import me.ilker.transaction.add.AddTransactionNavigationEventInfo
import me.ilker.transaction.add.AddTransactionScreen
import me.ilker.transaction.add.AddTransactionSideEffect
import me.ilker.transaction.add.AddTransactionState
import me.ilker.transaction.transactions.TransactionState
import me.ilker.transaction.transactions.TransactionsScreen
import org.koin.compose.koinInject

@Composable
fun CommonApp() {
    MaterialTheme {
        val navController: NavHostController = rememberNavController()
        val sdk: BalanceTrackerSDK = koinInject()

        NavHost(navController = navController, startDestination = Route.Root) {
            composable<Route.Root> {
                val manager = remember { TransactionManager(sdk = sdk) }
                val state: State<TransactionState> = manager.state.collectAsStateWithLifecycle()

                TransactionsScreen(
                    state = state,
                    add = { navController.navigate(Route.Add) }
                )
            }

            composable<Route.Add> { navBackStackEntry ->
                val route = navBackStackEntry.toRoute<Route.Add>()
                val manager = remember { AddTransactionManager(sdk = sdk) }
                val state: State<AddTransactionState> = manager.state.collectAsStateWithLifecycle()
                val sideEffects: Flow<AddTransactionSideEffect> = manager.sideEffect.receiveAsFlow()
                val navEventState = rememberNavigationEventState(
                    currentInfo = AddTransactionNavigationEventInfo(route = route),
                )

                NavigationBackHandler(
                    state = navEventState,
                    isBackEnabled = true,
                    onBackCompleted = { navController.popBackStack() }
                )

                AddTransactionScreen(
                    state = state,
                    sideEffects = sideEffects,
                    onAdd = { amount, dateTime ->
                        manager.sendIntent(AddTransactionIntent.Add(amount = amount, dateTime = dateTime))
                    }
                )
            }
        }
    }
}
