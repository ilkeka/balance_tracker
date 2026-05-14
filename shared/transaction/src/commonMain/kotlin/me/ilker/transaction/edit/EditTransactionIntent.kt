package me.ilker.transaction.edit

import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.core.Intent

sealed interface EditTransactionIntent: Intent {
    data object DeleteTransaction : EditTransactionIntent

    data class Edit(
        val amount: Double,
        val dateTime: String,
        val type: TransactionType,
        val category: TransactionCategory,
        val description: String?
    ): EditTransactionIntent
}
