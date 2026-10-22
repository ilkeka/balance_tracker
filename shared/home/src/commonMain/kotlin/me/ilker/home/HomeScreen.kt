package me.ilker.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.datetime.YearMonth
import me.ilker.home.views.HomeInitialView
import me.ilker.home.views.HomeLoadedView

@ExperimentalMaterial3Api
@Composable
fun HomeScreen(
    state: State<HomeState>,
    setSelectedYearMonth: (yearMonth: YearMonth) -> Unit,
    add: () -> Unit,
    onTransactionsClicked: () -> Unit,
    onClick: (id: Long) -> Unit,
    onSync: () -> Unit
) {
    when (val currentState = state.value) {
        HomeState.InitialState -> HomeInitialView(
            device = currentState.device,
            onSync = onSync
        )
        is HomeState.Loaded -> HomeLoadedView(
            state = currentState,
            device = currentState.device,
            setSelectedYearMonth = setSelectedYearMonth,
            add = add,
            onTransactionsClicked = onTransactionsClicked,
            onClick = onClick,
            onSync = onSync
        )
    }
}
