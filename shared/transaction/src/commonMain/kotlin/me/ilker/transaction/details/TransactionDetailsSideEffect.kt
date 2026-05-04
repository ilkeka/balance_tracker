package me.ilker.transaction.details

import me.ilker.core.SideEffect

sealed interface TransactionDetailsSideEffect: SideEffect {
    data class TransactionDeleted(
        val text: String
    ) : TransactionDetailsSideEffect

    data object Back : TransactionDetailsSideEffect

    data class Edit(
        val id: Long
    ) : TransactionDetailsSideEffect

    data class ShowError(
        val code: ErrorCode
    ) : TransactionDetailsSideEffect {
        enum class ErrorCode(open val code: Int) {
            NOT_FOUND(90001)
        }
    }
}
