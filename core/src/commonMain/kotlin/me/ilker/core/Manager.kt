package me.ilker.core

import kotlinx.coroutines.flow.StateFlow

abstract class Manager<STATE: State, INTENT: Intent, SIDE_EFFECT: SideEffect> {

    abstract fun sendIntent(intent: INTENT)

    abstract val state: StateFlow<STATE>
}
