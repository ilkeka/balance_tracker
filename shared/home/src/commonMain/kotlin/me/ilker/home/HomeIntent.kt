package me.ilker.home

import me.ilker.core.Intent

sealed interface HomeIntent : Intent {
    data class OnClick(
        val id: Long
    ): HomeIntent
}
