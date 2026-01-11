package me.ilker.transaction.transactions

import me.ilker.core.Intent

sealed interface TransactionIntent : Intent {
    data class Add(
        val amount: Double,
        val dateTime: String,
        val type: TransactionType,
        val description: String?
    ): TransactionIntent

    data class OnClick(
        val id: Long
    ): TransactionIntent

    data object OnDismissRequest: TransactionIntent
}
