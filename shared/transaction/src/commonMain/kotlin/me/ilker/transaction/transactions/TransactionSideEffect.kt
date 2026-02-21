package me.ilker.transaction.transactions

import me.ilker.core.SideEffect

sealed interface TransactionSideEffect : SideEffect {
    data class NavigateToTransactionDetails(val id: Long): TransactionSideEffect
}
