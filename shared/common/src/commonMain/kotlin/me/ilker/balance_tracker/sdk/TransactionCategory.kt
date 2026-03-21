package me.ilker.balance_tracker.sdk

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
