package me.ilker.transaction.transactions

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.core.State

sealed class TransactionState : State {
    data object InitialState: TransactionState()

    data class Loaded(
        val yearMonth: YearMonth,
        val balance: BalanceUiModel?,
        val categoryTotals: List<CategoryTotalUiModel>,
        val transactions: Map<LocalDate, List<TransactionDomainModel>>
    ) : TransactionState() {
        data class BalanceUiModel(
            val balance: Double,
            val expense: Double,
            val income: Double,
        )

        data class CategoryTotalUiModel(
            val category: TransactionCategory,
            val type: TransactionType,
            val amount: Double
        )
    }
}

sealed class ModalBottomSheetState {
    data class ShowOptions(
        val transactionId: Long
    ) : ModalBottomSheetState()
}
