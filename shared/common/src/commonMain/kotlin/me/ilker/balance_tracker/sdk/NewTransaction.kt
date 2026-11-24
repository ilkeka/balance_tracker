package me.ilker.balance_tracker.sdk

/**
 * A transaction that has been read from an external source but is not stored yet.
 *
 * [dateTime] uses the same `dd/MM/yyyy` format the database stores, so a row can be written without
 * a second conversion at the call site.
 */
data class NewTransaction(
    val amount: Double,
    val dateTime: String,
    val type: TransactionType,
    val category: TransactionCategory,
    val description: String?
)

/** What an import did, split so the UI can distinguish new rows from ones already present. */
data class ImportTransactionsResult(
    val imported: Int,
    val skipped: Int
)
