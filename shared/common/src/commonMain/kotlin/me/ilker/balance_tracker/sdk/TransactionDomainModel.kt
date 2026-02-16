package me.ilker.balance_tracker.sdk

data class TransactionDomainModel(
    val id: Long,
    val amount: Double,
    val dateTime: String,
    val type: TransactionType,
    val description: String?
)