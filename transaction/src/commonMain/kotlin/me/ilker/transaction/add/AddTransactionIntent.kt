package me.ilker.transaction.add

import me.ilker.core.Intent
import me.ilker.transaction.transactions.TransactionType

sealed class AddTransactionIntent : Intent {
    data class Add(
        val amount: Double,
        val dateTime: String,
        val type: TransactionType
    ): AddTransactionIntent()
}
