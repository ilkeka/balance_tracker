package me.ilker.balance_tracker.sdk

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType {
    Expense,
    Income
}