package me.ilker.balance_tracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import me.ilker.core.Manager
import me.ilker.core.ManagerLifecycleEvent

typealias ManagerStore = MutableMap<String, Manager<*, *, *>>

@Composable
fun <M : Manager<*, *, *>> rememberManager(
    entry: NavBackStackEntry,
    store: ManagerStore,
    factory: () -> M
): M {
    @Suppress("UNCHECKED_CAST")
    val manager = remember(entry.id) {
        store.getOrPut(entry.id) { factory() } as M
    }

    DisposableEffect(entry) {
        val lifecycle = entry.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            event.toManagerLifecycleEvent()?.let { manager.onLifecycleEvent(it) }
            if (event == Lifecycle.Event.ON_DESTROY) {
                store.remove(entry.id)?.close()
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            manager.onLifecycleEvent(ManagerLifecycleEvent.Resumed)
        }
        onDispose { lifecycle.removeObserver(observer) }
    }

    return manager
}

private fun Lifecycle.Event.toManagerLifecycleEvent(): ManagerLifecycleEvent? = when (this) {
    Lifecycle.Event.ON_CREATE -> ManagerLifecycleEvent.Created
    Lifecycle.Event.ON_START -> ManagerLifecycleEvent.Started
    Lifecycle.Event.ON_RESUME -> ManagerLifecycleEvent.Resumed
    Lifecycle.Event.ON_PAUSE -> ManagerLifecycleEvent.Paused
    Lifecycle.Event.ON_STOP -> ManagerLifecycleEvent.Stopped
    Lifecycle.Event.ON_DESTROY -> ManagerLifecycleEvent.Destroyed
    else -> null
}