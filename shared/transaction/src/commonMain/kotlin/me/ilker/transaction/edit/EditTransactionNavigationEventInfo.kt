package me.ilker.transaction.edit

import androidx.navigationevent.NavigationEventInfo
import me.ilker.core.Route

data class EditTransactionNavigationEventInfo(
    val route: Route
): NavigationEventInfo()
