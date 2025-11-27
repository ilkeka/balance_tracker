package me.ilker.core

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.internal.ChannelFlow

abstract class Manager<STATE: State, INTENT: Intent, SIDE_EFFECT: SideEffect> {

    abstract fun sendIntent(intent: INTENT)

    abstract val state: StateFlow<STATE>

    abstract val sideEffect: Channel<SIDE_EFFECT>
}
