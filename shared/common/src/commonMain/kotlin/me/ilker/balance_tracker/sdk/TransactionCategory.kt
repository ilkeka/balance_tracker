package me.ilker.balance_tracker.sdk

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

@Serializable
sealed interface TransactionCategory {
    val value: String

    @Serializable
    enum class Predefined(override val value: String) : TransactionCategory {
        Bill("bill"),
        Entertainment("entertainment"),
        Gift("gift"),
        Grocery("grocery"),
        Health("health"),
        Other("other"),
        Reimbursement("reimbursement"),
        Salary("salary"),
        Shopping("shopping"),
        Subscription("subscription"),
        Transportation("transportation"),
        Travel("travel")
    }

    @Serializable
    data class Custom(override val value: String) : TransactionCategory
}

@Composable
fun TransactionCategory.getValueForUI() = when (this) {
    TransactionCategory.Predefined.Bill,
    TransactionCategory.Predefined.Entertainment,
    TransactionCategory.Predefined.Gift,
    TransactionCategory.Predefined.Grocery,
    TransactionCategory.Predefined.Health,
    TransactionCategory.Predefined.Other,
    TransactionCategory.Predefined.Reimbursement,
    TransactionCategory.Predefined.Salary,
    TransactionCategory.Predefined.Shopping,
    TransactionCategory.Predefined.Subscription,
    TransactionCategory.Predefined.Transportation,
    TransactionCategory.Predefined.Travel,
    is TransactionCategory.Custom -> this.value
}
