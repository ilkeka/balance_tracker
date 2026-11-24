package me.ilker.data.csv

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import me.ilker.balance_tracker.sdk.NewTransaction
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.TransactionType
import kotlin.math.abs
import kotlin.math.roundToLong

/** The outcome of reading a CSV file. */
internal sealed interface CsvDocument {
    data class Parsed(
        val transactions: List<NewTransaction>,
        /** Rows that were present but could not be understood. */
        val invalidRows: Int
    ) : CsvDocument

    /** The header is missing or does not name the columns an export writes. */
    data object Unreadable : CsvDocument
}

/**
 * The CSV format this app writes and reads.
 *
 * Columns are `date,amount,type,category,description`. Values are the stable identifiers the
 * database stores (`expense`, `grocery`, `rent_mortgage`), not the localized labels shown in the UI,
 * so a file keeps its meaning when the app language changes or when it is edited in a spreadsheet.
 * Dates use the app's `dd/MM/yyyy` format and amounts are plain decimals with a dot.
 */
internal object TransactionCsv {

    const val Header = "date,amount,type,category,description"

    private const val DateColumn = "date"
    private const val AmountColumn = "amount"
    private const val TypeColumn = "type"
    private const val CategoryColumn = "category"
    private const val DescriptionColumn = "description"

    private val requiredColumns = listOf(DateColumn, AmountColumn, TypeColumn, CategoryColumn)

    private val DateFormat = LocalDate.Format {
        day()
        char('/')
        monthNumber()
        char('/')
        year()
    }

    fun encode(transactions: List<TransactionDomainModel>): String = buildString {
        append(Header)
        append("\r\n")

        transactions.forEach { transaction ->
            append(transaction.dateTime)
            append(',')
            append(formatAmount(transaction.amount))
            append(',')
            append(transaction.type.toCsvValue())
            append(',')
            append(escape(transaction.category.value))
            append(',')
            append(escape(transaction.description.orEmpty()))
            append("\r\n")
        }
    }

    fun decode(contents: String): CsvDocument {
        val records = CsvReader(contents.removePrefix("\uFEFF")).readRecords()
        val header = records.firstOrNull()?.map { column -> column.trim().lowercase() }
            ?: return CsvDocument.Unreadable

        if (requiredColumns.any { column -> column !in header }) {
            return CsvDocument.Unreadable
        }

        val indexes = ColumnIndexes(
            date = header.indexOf(DateColumn),
            amount = header.indexOf(AmountColumn),
            type = header.indexOf(TypeColumn),
            category = header.indexOf(CategoryColumn),
            description = header.indexOf(DescriptionColumn)
        )

        val rows = records.drop(1)
        val transactions = rows.mapNotNull { row -> parseRow(row = row, indexes = indexes) }

        return CsvDocument.Parsed(
            transactions = transactions,
            invalidRows = rows.size - transactions.size
        )
    }

    private fun parseRow(row: List<String>, indexes: ColumnIndexes): NewTransaction? {
        val date = parseDate(row.getOrNull(indexes.date)) ?: return null
        val amount = parseAmount(row.getOrNull(indexes.amount)) ?: return null
        val type = parseType(row.getOrNull(indexes.type)) ?: return null
        val category = parseCategory(row.getOrNull(indexes.category)) ?: return null
        val description = indexes.description
            .takeIf { index -> index >= 0 }
            ?.let(row::getOrNull)
            ?.trim()
            ?.takeUnless { value -> value.isEmpty() }

        return NewTransaction(
            amount = amount,
            dateTime = date,
            type = type,
            category = category,
            description = description
        )
    }

    /**
     * Accepts both padded and unpadded days and months, then stores the padded form the database
     * uses. The ISO round trip validates the date, so 31/02 is rejected rather than normalized.
     */
    private fun parseDate(value: String?): String? {
        val parts = value?.trim()?.split('/') ?: return null
        if (parts.size != 3) return null

        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull() ?: return null
        if (year !in 1..9999) return null

        val isoDate = "$year-${month.padded()}-${day.padded()}"
        return runCatching { LocalDate.parse(isoDate) }
            .getOrNull()
            ?.format(DateFormat)
    }

    /** A quoted `10,42` is accepted as well as `10.42`, which is what European spreadsheets emit. */
    private fun parseAmount(value: String?): Double? = value
        ?.trim()
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?.takeIf { amount -> amount.isFinite() }
        ?.let { amount -> (amount * 100).roundToLong() / 100.0 }

    private fun parseType(value: String?): TransactionType? = when (value?.trim()?.lowercase()) {
        "expense" -> TransactionType.Expense
        "income" -> TransactionType.Income
        else -> null
    }

    /**
     * A known value maps to its predefined category; anything else is kept as a custom category,
     * because the app supports those and refusing them would lose data on a round trip.
     */
    private fun parseCategory(value: String?): TransactionCategory? {
        val trimmed = value?.trim()?.takeUnless { it.isEmpty() } ?: return null
        return TransactionCategory.Predefined.entries.firstOrNull { category ->
            category.value.equals(trimmed, ignoreCase = true) ||
                category.name.equals(trimmed, ignoreCase = true)
        } ?: TransactionCategory.Custom(trimmed)
    }

    private fun TransactionType.toCsvValue(): String = when (this) {
        TransactionType.Expense -> "expense"
        TransactionType.Income -> "income"
    }

    /**
     * Money is written with exactly two decimals and no exponent, so every spreadsheet reads it as a
     * number and a round trip compares equal to the stored value.
     */
    private fun formatAmount(value: Double): String {
        val cents = (value * 100).roundToLong()
        val sign = if (cents < 0) "-" else ""
        val absolute = abs(cents)
        return buildString {
            append(sign)
            append(absolute / 100)
            append('.')
            append((absolute % 100).toString().padStart(2, '0'))
        }
    }

    private fun escape(value: String): String {
        val needsQuotes = value.any { char -> char == ',' || char == '"' || char == '\n' || char == '\r' } ||
            value != value.trim()
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun Int.padded(): String = toString().padStart(2, '0')

    private data class ColumnIndexes(
        val date: Int,
        val amount: Int,
        val type: Int,
        val category: Int,
        val description: Int
    )
}
