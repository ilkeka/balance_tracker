package me.ilker.transaction.add

import me.ilker.core.SideEffect

sealed class AddTransactionSideEffect : SideEffect {
    data class Feedback(
        val text: String
    ): AddTransactionSideEffect()

    data object Back: AddTransactionSideEffect()
}