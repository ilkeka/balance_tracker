package me.ilker.balance_tracker.sdk

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.ui.graphics.vector.ImageVector

fun TransactionCategory.getIcon(): ImageVector = when (this) {
    TransactionCategory.Predefined.Bill -> Icons.Rounded.Receipt
    TransactionCategory.Predefined.Children -> Icons.Rounded.ChildCare
    TransactionCategory.Predefined.Debt -> Icons.Rounded.CreditCard
    TransactionCategory.Predefined.Dining -> Icons.Rounded.Restaurant
    TransactionCategory.Predefined.Education -> Icons.Rounded.School
    TransactionCategory.Predefined.Entertainment -> Icons.Rounded.Movie
    TransactionCategory.Predefined.Fitness -> Icons.Rounded.FitnessCenter
    TransactionCategory.Predefined.Gift -> Icons.Rounded.CardGiftcard
    TransactionCategory.Predefined.Grocery -> Icons.Rounded.ShoppingCart
    TransactionCategory.Predefined.Health -> Icons.Rounded.Favorite
    TransactionCategory.Predefined.Insurance -> Icons.Rounded.Shield
    TransactionCategory.Predefined.Other -> Icons.Rounded.MoreHoriz
    TransactionCategory.Predefined.PersonalCare -> Icons.Rounded.Spa
    TransactionCategory.Predefined.Pets -> Icons.Rounded.Pets
    TransactionCategory.Predefined.Reimbursement -> Icons.Rounded.CurrencyExchange
    TransactionCategory.Predefined.RentMortgage -> Icons.Rounded.Home
    TransactionCategory.Predefined.Salary -> Icons.Rounded.Payments
    TransactionCategory.Predefined.Shopping -> Icons.Rounded.ShoppingBag
    TransactionCategory.Predefined.Subscription -> Icons.Rounded.Autorenew
    TransactionCategory.Predefined.Taxes -> Icons.Rounded.AccountBalance
    TransactionCategory.Predefined.Transportation -> Icons.Rounded.DirectionsCar
    TransactionCategory.Predefined.Travel -> Icons.Rounded.Flight
    TransactionCategory.Predefined.Utilities -> Icons.Rounded.Bolt
    is TransactionCategory.Custom -> Icons.AutoMirrored.Rounded.Label
}
