package me.ilker.balance_tracker.sdk

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.char

data class TransactionDomainModel(
    val id: Long,
    val amount: Double,
    val dateTime: String,
    val type: TransactionType,
    val category: TransactionCategory,
    val description: String?
)

fun TransactionDomainModel.getLocalDate() = LocalDate.parse(
    input = dateTime,
    format = LocalDate.Format {
        day()
        char('/')
        monthNumber()
        char('/')
        year()
    }
)
