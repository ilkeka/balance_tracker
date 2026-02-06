package me.ilker.balance_tracker

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import me.ilker.balance_tracker.theme.AppTheme
import me.ilker.core.Route
import me.ilker.transaction.add.AddTransactionIntent
import me.ilker.transaction.add.AddTransactionNavigationEventInfo
import me.ilker.transaction.add.AddTransactionScreen
import me.ilker.transaction.add.AddTransactionSideEffect
import me.ilker.transaction.add.AddTransactionState
import me.ilker.transaction.transactions.TransactionIntent
import me.ilker.transaction.transactions.TransactionState
import me.ilker.transaction.transactions.TransactionsScreen
import org.koin.compose.koinInject

@ExperimentalMaterial3Api
@Composable
fun CommonApp() {
    AppTheme {
        val navController: NavHostController = rememberNavController()
        val sdk: BalanceTrackerSDK = koinInject()

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { padding ->
            NavHost(
                modifier = Modifier.consumeWindowInsets(padding),
                navController = navController,
                startDestination = Route.Root
            ) {
                composable<Route.Root> {
                    val manager = remember { TransactionManager(sdk = sdk) }
                    val state: State<TransactionState> = manager.state.collectAsStateWithLifecycle()

                    TransactionsScreen(
                        state = state,
                        add = { navController.navigate(Route.Add) },
                        onDeleteTransactions = { manager.sendIntent(TransactionIntent.OnDeleteTransaction) },
                        onDismissRequest = { manager.sendIntent(TransactionIntent.OnDismissRequest) },
                        onClick = { id -> manager.sendIntent(TransactionIntent.OnClick(id = id)) }
                    )
                }

                composable<Route.Add> { navBackStackEntry ->
                    val route = navBackStackEntry.toRoute<Route.Add>()
                    val manager = remember { AddTransactionManager(sdk = sdk) }
                    val state: State<AddTransactionState> =
                        manager.state.collectAsStateWithLifecycle()
                    val sideEffects: Flow<AddTransactionSideEffect> =
                        manager.sideEffect.receiveAsFlow()
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
                        onAdd = { amount, dateTime, type, description ->
                            manager.sendIntent(
                                AddTransactionIntent.Add(
                                    amount = amount,
                                    dateTime = dateTime,
                                    type = type,
                                    description = description
                                )
                            )
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
