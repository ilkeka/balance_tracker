package me.ilker.transaction.edit

import kotlinx.serialization.Serializable
import me.ilker.core.Route

@Serializable
data class EditTransaction(
    val id: Long
) : Route