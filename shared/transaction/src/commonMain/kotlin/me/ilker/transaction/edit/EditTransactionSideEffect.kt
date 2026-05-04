package me.ilker.transaction.edit

import me.ilker.core.SideEffect

sealed interface EditTransactionSideEffect: SideEffect {
    data class TransactionDeleted(
        val text: String
    ) : EditTransactionSideEffect

    data object Back : EditTransactionSideEffect
}
