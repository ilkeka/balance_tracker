package me.ilker.sync.protocol

import kotlinx.serialization.Serializable
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sync.SyncedTransaction

/**
 * Wire representation of a transaction.
 *
 * Deliberately flat strings rather than the `TransactionCategory` enum: the category is a sealed
 * hierarchy whose main branch is an enum, which cannot be encoded polymorphically, and pinning the
 * wire format to the same stable string the `Transactions` table already stores keeps a future
 * category rename from breaking sync with an older build.
 */
@Serializable
internal data class SyncedTransactionDto(
    val syncId: String,
    val amount: Double,
    val dateTime: String,
    val type: String,
    val category: String,
    val description: String?,
    val version: Long,
    val originId: String,
    val isDeleted: Boolean
)

internal fun SyncedTransaction.toDto() = SyncedTransactionDto(
    syncId = syncId,
    amount = amount,
    dateTime = dateTime,
    type = type.name,
    category = category.value,
    description = description,
    version = version,
    originId = originId,
    isDeleted = isDeleted
)

internal fun SyncedTransactionDto.toSynced(): SyncedTransaction = SyncedTransaction(
    syncId = syncId,
    amount = amount,
    dateTime = dateTime,
    type = runCatching { TransactionType.valueOf(type) }.getOrDefault(TransactionType.Expense),
    category = TransactionCategory.Predefined.entries
        .find { it.value == category }
        ?: TransactionCategory.Custom(category),
    description = description,
    version = version,
    originId = originId,
    isDeleted = isDeleted
)