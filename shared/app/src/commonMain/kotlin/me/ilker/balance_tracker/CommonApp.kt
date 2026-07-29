package me.ilker.balance_tracker

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import me.ilker.balance_tracker.auth.authApi
import kotlinx.datetime.yearMonth
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.theme.AppTheme
import me.ilker.auth.Registration
import me.ilker.auth.RegistrationIntent
import me.ilker.auth.RegistrationManager
import me.ilker.auth.RegistrationScreen
import me.ilker.auth.RegistrationSideEffect
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
import me.ilker.transaction.details.navigation.TransactionDetails
import me.ilker.transaction.details.TransactionDetailsIntent
import me.ilker.transaction.details.TransactionDetailsScreen
import me.ilker.transaction.details.TransactionDetailsSideEffect
import me.ilker.transaction.details.TransactionDetailsState
import me.ilker.transaction.details.manager.TransactionDetailsManager
import me.ilker.transaction.details.navigation.TransactionDetailsNavigationEventInfo
import me.ilker.transaction.edit.EditTransaction
import me.ilker.transaction.edit.EditTransactionIntent
import me.ilker.transaction.edit.EditTransactionNavigationEventInfo
import me.ilker.transaction.edit.EditTransactionScreen
import me.ilker.transaction.edit.EditTransactionSideEffect
import me.ilker.transaction.edit.EditTransactionState
import me.ilker.transaction.edit.manager.EditTransactionManager
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
        val managerStore: ManagerStore = remember { mutableMapOf() }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
        ) { padding ->
            NavHost(
                modifier = Modifier
                    .consumeWindowInsets(padding),
                navController = navController,
                startDestination = Home,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(durationMillis = 0, easing = EaseIn)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(durationMillis = 0, easing = EaseOutBack)) },
                popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(durationMillis = 0, easing = EaseIn)) },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(durationMillis = 0, easing = EaseOutBack)) }
            ) {
                composable<Home> { navBackStackEntry ->
                    val manager = rememberManager(entry = navBackStackEntry, store = managerStore) {
                        HomeManager(sdk = sdk)
                    }
                    val state = manager.state.collectAsStateWithLifecycle()

                    HomeScreen(
                        state = state,
                        setSelectedYearMonth = { value -> manager.sendIntent(HomeIntent.SetSelectedYearMonth(yearMonth = value)) },
                        add = { navController.navigate(AddTransaction) },
                        onTransactionsClicked = { navController.navigate(Transactions(yearMonth = state.value.selectedDate.yearMonth.toString())) },
                        onClick = { id -> navController.navigate(TransactionDetails(id = id)) },
                        onRegister = { navController.navigate(Registration) },
                    )
                }

                composable<Transactions> { navBackStackEntry ->
                    val route = navBackStackEntry.toRoute<Transactions>()
                    val manager = rememberManager(entry = navBackStackEntry, store = managerStore) {
                        TransactionManager(
                            sdk = sdk,
                            yearMonth = route.yearMonth
                        )
                    }
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
                        onClick = { id -> navController.navigate(TransactionDetails(id = id)) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<AddTransaction> { navBackStackEntry ->
                    val route = navBackStackEntry.toRoute<AddTransaction>()
                    val manager = rememberManager(
                        entry = navBackStackEntry,
                        store = managerStore
                    ) {
                        AddTransactionManager(sdk = sdk)
                    }
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
                        onAdd = { amount, dateTime, type, category, description ->
                            manager.sendIntent(
                                AddTransactionIntent.Add(
                                    amount = amount,
                                    dateTime = dateTime,
                                    type = type,
                                    category = category,
                                    description = description
                                )
                            )
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<TransactionDetails> { navBackStackEntry ->
                    val route = navBackStackEntry.toRoute<TransactionDetails>()
                    val manager = rememberManager(entry = navBackStackEntry, store = managerStore) {
                        TransactionDetailsManager(
                            id = route.id,
                            sdk = sdk
                        )
                    }
                    val state: State<TransactionDetailsState> = manager.state.collectAsStateWithLifecycle()
                    val sideEffects: Flow<TransactionDetailsSideEffect> = manager.sideEffect.receiveAsFlow()
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
                        sideEffects = sideEffects,
                        onEdit = { id -> navController.navigate(EditTransaction(id = id)) },
                        onEditClicked = { id -> manager.sendIntent(TransactionDetailsIntent.EditTransaction(id = id)) },
                        onDelete = { manager.sendIntent(TransactionDetailsIntent.DeleteTransaction) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<EditTransaction> { navBackStackEntry ->
                    val route = navBackStackEntry.toRoute<EditTransaction>()
                    val manager = rememberManager(entry = navBackStackEntry, store = managerStore) {
                        EditTransactionManager(
                            id = route.id,
                            sdk = sdk
                        )
                    }
                    val state: State<EditTransactionState> = manager.state.collectAsStateWithLifecycle()
                    val sideEffects: Flow<EditTransactionSideEffect> = manager.sideEffect.receiveAsFlow()
                    val navEventState = rememberNavigationEventState(
                        currentInfo = EditTransactionNavigationEventInfo(route = route)
                    )

                    NavigationBackHandler(
                        state = navEventState,
                        isBackEnabled = true,
                        onBackCompleted = { navController.popBackStack() }
                    )

                    EditTransactionScreen(
                        state = state,
                        sideEffects = sideEffects,
                        onEdit = { amount, dateTime, type, category, description ->
                            manager.sendIntent(
                                EditTransactionIntent.Edit(
                                    amount = amount,
                                    dateTime = dateTime,
                                    type = type,
                                    category = category,
                                    description = description
                                )
                            )
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<Registration> { navBackStackEntry ->
                    val manager = rememberManager(entry = navBackStackEntry, store = managerStore) {
                        RegistrationManager()
                    }
                    val state = manager.state.collectAsStateWithLifecycle()
                    val sideEffects = manager.sideEffect.receiveAsFlow()

                    LaunchedEffect(Unit) {
                        sideEffects.collect { effect ->
                            when (effect) {
                                is RegistrationSideEffect.RegistrationComplete ->
                                    navController.popBackStack()
                            }
                        }
                    }

                    RegistrationScreen(
                        state = state,
                        onRegister = { email, password ->
                            manager.sendIntent(RegistrationIntent.Register(email = email, password = password))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
