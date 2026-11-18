package me.ilker.balance_tracker.sdk

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.category_bill
import me.ilker.balance_tracker.resources.category_children
import me.ilker.balance_tracker.resources.category_debt
import me.ilker.balance_tracker.resources.category_dining
import me.ilker.balance_tracker.resources.category_education
import me.ilker.balance_tracker.resources.category_entertainment
import me.ilker.balance_tracker.resources.category_fitness
import me.ilker.balance_tracker.resources.category_gift
import me.ilker.balance_tracker.resources.category_grocery
import me.ilker.balance_tracker.resources.category_health
import me.ilker.balance_tracker.resources.category_insurance
import me.ilker.balance_tracker.resources.category_other
import me.ilker.balance_tracker.resources.category_personal_care
import me.ilker.balance_tracker.resources.category_pets
import me.ilker.balance_tracker.resources.category_reimbursement
import me.ilker.balance_tracker.resources.category_rent_mortgage
import me.ilker.balance_tracker.resources.category_salary
import me.ilker.balance_tracker.resources.category_shopping
import me.ilker.balance_tracker.resources.category_subscription
import me.ilker.balance_tracker.resources.category_taxes
import me.ilker.balance_tracker.resources.category_transportation
import me.ilker.balance_tracker.resources.category_travel
import me.ilker.balance_tracker.resources.category_utilities
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Serializable
sealed interface TransactionCategory {
    val value: String

    @Serializable
    enum class Predefined(override val value: String) : TransactionCategory {
        Bill("bill"),
        Children("children"),
        Debt("debt"),
        Dining("dining"),
        Education("education"),
        Entertainment("entertainment"),
        Fitness("fitness"),
        Gift("gift"),
        Grocery("grocery"),
        Health("health"),
        Insurance("insurance"),
        Other("other"),
        PersonalCare("personal_care"),
        Pets("pets"),
        Reimbursement("reimbursement"),
        RentMortgage("rent_mortgage"),
        Salary("salary"),
        Shopping("shopping"),
        Subscription("subscription"),
        Taxes("taxes"),
        Transportation("transportation"),
        Travel("travel"),
        Utilities("utilities")
    }

    @Serializable
    data class Custom(override val value: String) : TransactionCategory
}

@Composable
fun TransactionCategory.getValueForComposableUI() = when (this) {
    TransactionCategory.Predefined.Bill -> stringResource(Res.string.category_bill)
    TransactionCategory.Predefined.Children -> stringResource(Res.string.category_children)
    TransactionCategory.Predefined.Debt -> stringResource(Res.string.category_debt)
    TransactionCategory.Predefined.Dining -> stringResource(Res.string.category_dining)
    TransactionCategory.Predefined.Education -> stringResource(Res.string.category_education)
    TransactionCategory.Predefined.Entertainment -> stringResource(Res.string.category_entertainment)
    TransactionCategory.Predefined.Fitness -> stringResource(Res.string.category_fitness)
    TransactionCategory.Predefined.Gift -> stringResource(Res.string.category_gift)
    TransactionCategory.Predefined.Grocery -> stringResource(Res.string.category_grocery)
    TransactionCategory.Predefined.Health -> stringResource(Res.string.category_health)
    TransactionCategory.Predefined.Insurance -> stringResource(Res.string.category_insurance)
    TransactionCategory.Predefined.Other -> stringResource(Res.string.category_other)
    TransactionCategory.Predefined.PersonalCare -> stringResource(Res.string.category_personal_care)
    TransactionCategory.Predefined.Pets -> stringResource(Res.string.category_pets)
    TransactionCategory.Predefined.Reimbursement -> stringResource(Res.string.category_reimbursement)
    TransactionCategory.Predefined.RentMortgage -> stringResource(Res.string.category_rent_mortgage)
    TransactionCategory.Predefined.Salary -> stringResource(Res.string.category_salary)
    TransactionCategory.Predefined.Shopping -> stringResource(Res.string.category_shopping)
    TransactionCategory.Predefined.Subscription -> stringResource(Res.string.category_subscription)
    TransactionCategory.Predefined.Taxes -> stringResource(Res.string.category_taxes)
    TransactionCategory.Predefined.Transportation -> stringResource(Res.string.category_transportation)
    TransactionCategory.Predefined.Travel -> stringResource(Res.string.category_travel)
    TransactionCategory.Predefined.Utilities -> stringResource(Res.string.category_utilities)
    is TransactionCategory.Custom -> this.value
}

suspend fun TransactionCategory.getValueForUI() = when (this) {
    TransactionCategory.Predefined.Bill -> getString(Res.string.category_bill)
    TransactionCategory.Predefined.Children -> getString(Res.string.category_children)
    TransactionCategory.Predefined.Debt -> getString(Res.string.category_debt)
    TransactionCategory.Predefined.Dining -> getString(Res.string.category_dining)
    TransactionCategory.Predefined.Education -> getString(Res.string.category_education)
    TransactionCategory.Predefined.Entertainment -> getString(Res.string.category_entertainment)
    TransactionCategory.Predefined.Fitness -> getString(Res.string.category_fitness)
    TransactionCategory.Predefined.Gift -> getString(Res.string.category_gift)
    TransactionCategory.Predefined.Grocery -> getString(Res.string.category_grocery)
    TransactionCategory.Predefined.Health -> getString(Res.string.category_health)
    TransactionCategory.Predefined.Insurance -> getString(Res.string.category_insurance)
    TransactionCategory.Predefined.Other -> getString(Res.string.category_other)
    TransactionCategory.Predefined.PersonalCare -> getString(Res.string.category_personal_care)
    TransactionCategory.Predefined.Pets -> getString(Res.string.category_pets)
    TransactionCategory.Predefined.Reimbursement -> getString(Res.string.category_reimbursement)
    TransactionCategory.Predefined.RentMortgage -> getString(Res.string.category_rent_mortgage)
    TransactionCategory.Predefined.Salary -> getString(Res.string.category_salary)
    TransactionCategory.Predefined.Shopping -> getString(Res.string.category_shopping)
    TransactionCategory.Predefined.Subscription -> getString(Res.string.category_subscription)
    TransactionCategory.Predefined.Taxes -> getString(Res.string.category_taxes)
    TransactionCategory.Predefined.Transportation -> getString(Res.string.category_transportation)
    TransactionCategory.Predefined.Travel -> getString(Res.string.category_travel)
    TransactionCategory.Predefined.Utilities -> getString(Res.string.category_utilities)
    is TransactionCategory.Custom -> this.value
}

fun TransactionCategory.Predefined.isAvailableFor(type: TransactionType): Boolean = when (type) {
    TransactionType.Income -> {
        this == TransactionCategory.Predefined.Gift ||
            this == TransactionCategory.Predefined.Other ||
            this == TransactionCategory.Predefined.Reimbursement ||
            this == TransactionCategory.Predefined.Salary
    }

    TransactionType.Expense -> {
        this != TransactionCategory.Predefined.Reimbursement &&
            this != TransactionCategory.Predefined.Salary
    }
}

fun TransactionType.availableCategories(): List<TransactionCategory.Predefined> =
    TransactionCategory.Predefined.entries.filter { category -> category.isAvailableFor(this) }

fun TransactionCategory.withFallbackFor(type: TransactionType): TransactionCategory =
    if (this is TransactionCategory.Predefined && !isAvailableFor(type)) {
        TransactionCategory.Predefined.Other
    } else {
        this
    }
