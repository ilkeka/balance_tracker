package me.ilker.transaction.add

import androidx.navigationevent.NavigationEventInfo
import me.ilker.core.Route

data class AddTransactionNavigationEventInfo(
    val route: Route
): NavigationEventInfo()
