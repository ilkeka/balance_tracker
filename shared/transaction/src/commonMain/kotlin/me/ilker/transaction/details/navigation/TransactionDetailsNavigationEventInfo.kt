package me.ilker.transaction.details.navigation

import androidx.navigationevent.NavigationEventInfo
import me.ilker.core.Route

data class TransactionDetailsNavigationEventInfo(
    val route: Route
): NavigationEventInfo()
