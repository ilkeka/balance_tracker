package me.ilker.transaction.add

import me.ilker.core.Intent
import me.ilker.balance_tracker.sdk.TransactionType

sealed class AddTransactionIntent : Intent {
    data class Add(
        val amount: Double,
        val dateTime: String,
        val type: TransactionType,
        val description: String?
    ): AddTransactionIntent()
}
