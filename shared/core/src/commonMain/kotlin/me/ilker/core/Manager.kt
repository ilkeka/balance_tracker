package me.ilker.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.EmptyCoroutineContext

abstract class Manager<STATE: State, INTENT: Intent, SIDE_EFFECT: SideEffect> {
    protected val scope: CoroutineScope = CoroutineScope(EmptyCoroutineContext + SupervisorJob())

    abstract fun sendIntent(intent: INTENT)

    abstract val state: StateFlow<STATE>

    abstract val sideEffect: Channel<SIDE_EFFECT>

    fun close() {
        scope.cancel()
        sideEffect.close()
    }
}
