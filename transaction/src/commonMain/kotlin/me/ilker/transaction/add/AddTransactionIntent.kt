package me.ilker.transaction.add

import me.ilker.core.Intent

sealed class AddTransactionIntent : Intent {
    data class Add(
        val amount: Double,
        val dateTime: String
    ): AddTransactionIntent()
}
