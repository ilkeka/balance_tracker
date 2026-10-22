package me.ilker.sync.navigation

import androidx.navigationevent.NavigationEventInfo
import me.ilker.core.Route

data class SyncNavigationEventInfo(
    val route: Route
): NavigationEventInfo()
