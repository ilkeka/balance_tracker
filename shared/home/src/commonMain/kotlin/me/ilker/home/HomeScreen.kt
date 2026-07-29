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
    sessionEmail: String?,
    setSelectedYearMonth: (yearMonth: YearMonth) -> Unit,
    add: () -> Unit,
    onTransactionsClicked: () -> Unit,
    onClick: (id: Long) -> Unit,
    onRegister: () -> Unit
) {
    when (val currentState = state.value) {
        HomeState.InitialState -> HomeInitialView(
            sessionEmail = sessionEmail,
            onRegister = onRegister
        )
        is HomeState.Loaded -> HomeLoadedView(
            state = currentState,
            sessionEmail = sessionEmail,
            setSelectedYearMonth = setSelectedYearMonth,
            add = add,
            onTransactionsClicked = onTransactionsClicked,
            onClick = onClick,
            onRegister = onRegister
        )
    }
}
