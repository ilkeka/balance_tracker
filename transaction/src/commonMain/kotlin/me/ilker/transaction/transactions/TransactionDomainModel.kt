package me.ilker.transaction.transactions

data class TransactionDomainModel(
    val id: Long,
    val amount: Double,
    val dateTime: String,
    val type: TransactionType
)
