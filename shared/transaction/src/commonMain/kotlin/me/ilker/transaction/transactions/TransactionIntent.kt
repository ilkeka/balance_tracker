package me.ilker.transaction.transactions

import me.ilker.core.Intent

sealed interface TransactionIntent : Intent {
    data class OnClick(
        val id: Long
    ): TransactionIntent

    data object OnDismissRequest: TransactionIntent

    data object OnDeleteTransaction: TransactionIntent
}
