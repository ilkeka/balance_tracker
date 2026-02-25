package me.ilker.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import me.ilker.home.views.HomeInitialView
import me.ilker.home.views.HomeLoadedView

@ExperimentalMaterial3Api
@Composable
fun HomeScreen(
    state: State<HomeState>,
    add: () -> Unit,
    onTransactionsClicked: () -> Unit,
    onClick: (id: Long) -> Unit
) {
    when (val currentState = state.value) {
        HomeState.InitialState -> HomeInitialView()
        is HomeState.Loaded -> HomeLoadedView(
            state = currentState,
            add = add,
            onTransactionsClicked = onTransactionsClicked,
            onClick = onClick
        )
    }
}
