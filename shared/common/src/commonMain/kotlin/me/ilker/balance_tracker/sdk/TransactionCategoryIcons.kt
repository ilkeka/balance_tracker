package me.ilker.balance_tracker.sdk

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

fun TransactionCategory.getIcon(): ImageVector = when (this) {
    TransactionCategory.Predefined.Bill -> Icons.Rounded.Receipt
    TransactionCategory.Predefined.Entertainment -> Icons.Rounded.Movie
    TransactionCategory.Predefined.Gift -> Icons.Rounded.CardGiftcard
    TransactionCategory.Predefined.Grocery -> Icons.Rounded.ShoppingCart
    TransactionCategory.Predefined.Health -> Icons.Rounded.Favorite
    TransactionCategory.Predefined.Other -> Icons.Rounded.MoreHoriz
    TransactionCategory.Predefined.Reimbursement -> Icons.Rounded.CurrencyExchange
    TransactionCategory.Predefined.Salary -> Icons.Rounded.Payments
    TransactionCategory.Predefined.Shopping -> Icons.Rounded.ShoppingBag
    TransactionCategory.Predefined.Subscription -> Icons.Rounded.Autorenew
    TransactionCategory.Predefined.Transportation -> Icons.Rounded.DirectionsCar
    TransactionCategory.Predefined.Travel -> Icons.Rounded.Flight
    is TransactionCategory.Custom -> Icons.AutoMirrored.Rounded.Label
}
