package me.ilker.balance_tracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import me.ilker.core.Manager

typealias  ManagerStore = MutableMap<String, Manager<*, *, *>>

@Composable
fun <M: Manager<*, *, *>> rememberManager(
    entry: NavBackStackEntry,
    store: ManagerStore,
    factory: () -> M
): M {
    @Suppress("UNCHECKED_CAST")
    val manager = remember(entry.id) {
        store.getOrPut(entry.id) { factory() } as M
    }

    DisposableEffect(entry) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                store.remove(entry.id)?.close()
            }
        }

        entry.lifecycle.addObserver(observer)
        onDispose { entry.lifecycle.removeObserver(observer) }
    }

    return manager
}
