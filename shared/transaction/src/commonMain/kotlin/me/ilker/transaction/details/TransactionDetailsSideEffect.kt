package me.ilker.transaction.details

import me.ilker.core.SideEffect

sealed interface TransactionDetailsSideEffect: SideEffect {
    data class TransactionDeleted(
        val text: String
    ) : TransactionDetailsSideEffect

    data object Back : TransactionDetailsSideEffect
}
