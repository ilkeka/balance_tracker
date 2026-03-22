package me.ilker.balance_tracker.sdk

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.category_bill
import me.ilker.balance_tracker.resources.category_entertainment
import me.ilker.balance_tracker.resources.category_gift
import me.ilker.balance_tracker.resources.category_grocery
import me.ilker.balance_tracker.resources.category_health
import me.ilker.balance_tracker.resources.category_other
import me.ilker.balance_tracker.resources.category_reimbursement
import me.ilker.balance_tracker.resources.category_salary
import me.ilker.balance_tracker.resources.category_shopping
import me.ilker.balance_tracker.resources.category_subscription
import me.ilker.balance_tracker.resources.category_transportation
import me.ilker.balance_tracker.resources.category_travel
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

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
fun TransactionCategory.getValueForComposableUI() = when (this) {
    TransactionCategory.Predefined.Bill -> stringResource(Res.string.category_bill)
    TransactionCategory.Predefined.Entertainment -> stringResource(Res.string.category_entertainment)
    TransactionCategory.Predefined.Gift -> stringResource(Res.string.category_gift)
    TransactionCategory.Predefined.Grocery -> stringResource(Res.string.category_grocery)
    TransactionCategory.Predefined.Health -> stringResource(Res.string.category_health)
    TransactionCategory.Predefined.Other -> stringResource(Res.string.category_other)
    TransactionCategory.Predefined.Reimbursement -> stringResource(Res.string.category_reimbursement)
    TransactionCategory.Predefined.Salary -> stringResource(Res.string.category_salary)
    TransactionCategory.Predefined.Shopping -> stringResource(Res.string.category_shopping)
    TransactionCategory.Predefined.Subscription -> stringResource(Res.string.category_subscription)
    TransactionCategory.Predefined.Transportation -> stringResource(Res.string.category_transportation)
    TransactionCategory.Predefined.Travel -> stringResource(Res.string.category_travel)
    is TransactionCategory.Custom -> this.value
}

suspend fun TransactionCategory.getValueForUI() = when (this) {
    TransactionCategory.Predefined.Bill -> getString(Res.string.category_bill)
    TransactionCategory.Predefined.Entertainment -> getString(Res.string.category_entertainment)
    TransactionCategory.Predefined.Gift -> getString(Res.string.category_gift)
    TransactionCategory.Predefined.Grocery -> getString(Res.string.category_grocery)
    TransactionCategory.Predefined.Health -> getString(Res.string.category_health)
    TransactionCategory.Predefined.Other -> getString(Res.string.category_other)
    TransactionCategory.Predefined.Reimbursement -> getString(Res.string.category_reimbursement)
    TransactionCategory.Predefined.Salary -> getString(Res.string.category_salary)
    TransactionCategory.Predefined.Shopping -> getString(Res.string.category_shopping)
    TransactionCategory.Predefined.Subscription -> getString(Res.string.category_subscription)
    TransactionCategory.Predefined.Transportation -> getString(Res.string.category_transportation)
    TransactionCategory.Predefined.Travel -> getString(Res.string.category_travel)
    is TransactionCategory.Custom -> this.value
}
