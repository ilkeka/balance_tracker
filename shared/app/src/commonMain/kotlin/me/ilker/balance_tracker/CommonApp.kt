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
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.theme.AppTheme
import me.ilker.home.Home
import me.ilker.home.HomeIntent
import me.ilker.home.HomeManager
import me.ilker.home.HomeScreen
import me.ilker.transaction.add.AddTransactionIntent
import me.ilker.transaction.add.AddTransactionScreen
import me.ilker.transaction.add.AddTransactionSideEffect
import me.ilker.transaction.add.AddTransactionState
import me.ilker.transaction.add.manager.AddTransactionManager
import me.ilker.transaction.add.navigation.AddTransaction
import me.ilker.transaction.add.navigation.AddTransactionNavigationEventInfo
import me.ilker.transaction.details.TransactionDetails
import me.ilker.transaction.details.TransactionDetailsScreen
import me.ilker.transaction.details.TransactionDetailsState
import me.ilker.transaction.details.manager.TransactionDetailsManager
import me.ilker.transaction.details.navigation.TransactionDetailsNavigationEventInfo
import me.ilker.transaction.transactions.TransactionState
import me.ilker.transaction.transactions.TransactionsScreen
import me.ilker.transaction.transactions.manager.TransactionManager
import me.ilker.transaction.transactions.navigation.Transactions
import me.ilker.transaction.transactions.navigation.TransactionsNavigationEventInfo
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
                startDestination = Home
            ) {
                composable<Home> {
                    val manager = remember { HomeManager(sdk = sdk) }
                    val state = manager.state.collectAsStateWithLifecycle()

                    HomeScreen(
                        state = state,
                        add = { navController.navigate(AddTransaction) },
                        onTransactionsClicked = { navController.navigate(Transactions) },
                        onClick = { id -> manager.sendIntent(HomeIntent.OnClick(id = id)) }
                    )
                }

                composable<Transactions> { navBackStackEntry ->
                    val route = navBackStackEntry.toRoute<AddTransaction>()
                    val manager = remember { TransactionManager(sdk = sdk) }
                    val state: State<TransactionState> = manager.state.collectAsStateWithLifecycle()
                    val navEventState = rememberNavigationEventState(
                        currentInfo = TransactionsNavigationEventInfo(route = route),
                    )

                    NavigationBackHandler(
                        state = navEventState,
                        isBackEnabled = true,
                        onBackCompleted = { navController.popBackStack() }
                    )

                    TransactionsScreen(
                        state = state,
                        add = { navController.navigate(AddTransaction) },
                        onClick = { id -> navController.navigate(TransactionDetails(id = id)) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<AddTransaction> { navBackStackEntry ->
                    val route = navBackStackEntry.toRoute<AddTransaction>()
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

                composable<TransactionDetails> { navBackStackEntry ->
                    val route = navBackStackEntry.toRoute<TransactionDetails>()
                    val manager = remember {
                        TransactionDetailsManager(
                            id = route.id,
                            sdk = sdk
                        )
                    }
                    val state: State<TransactionDetailsState> = manager.state.collectAsStateWithLifecycle()
                    val navEventState = rememberNavigationEventState(
                        currentInfo = TransactionDetailsNavigationEventInfo(route = route),
                    )

                    NavigationBackHandler(
                        state = navEventState,
                        isBackEnabled = true,
                        onBackCompleted = { navController.popBackStack() }
                    )

                    TransactionDetailsScreen(
                        state = state,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
