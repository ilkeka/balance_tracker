package me.ilker.transaction.details.navigation

import kotlinx.serialization.Serializable
import me.ilker.core.Route

@Serializable
data class TransactionDetails(
    val id: Long
) : Route