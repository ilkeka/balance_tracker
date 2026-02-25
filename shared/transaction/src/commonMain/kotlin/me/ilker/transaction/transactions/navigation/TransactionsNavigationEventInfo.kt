package me.ilker.transaction.transactions.navigation

import androidx.navigationevent.NavigationEventInfo
import me.ilker.core.Route

data class TransactionsNavigationEventInfo(
    val route: Route
): NavigationEventInfo()