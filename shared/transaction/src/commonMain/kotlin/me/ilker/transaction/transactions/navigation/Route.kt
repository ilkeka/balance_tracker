package me.ilker.transaction.transactions.navigation

import kotlinx.datetime.YearMonth
import kotlinx.serialization.Serializable
import me.ilker.core.Route

@Serializable
data class Transactions(
    val yearMonth: String
) : Route