package me.ilker.transaction.transactions.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionEntity(
    @SerialName("id")
    val id: Int,

    @SerialName("amount")
    val amount: Double,

    @SerialName("datetime")
    val dateTime: String,
)
