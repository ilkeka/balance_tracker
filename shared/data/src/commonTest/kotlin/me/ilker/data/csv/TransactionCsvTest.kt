package me.ilker.data.csv

import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TransactionCsvTest {

    private fun transaction(
        id: Long = 1,
        amount: Double = 12.5,
        dateTime: String = "01/02/2026",
        type: TransactionType = TransactionType.Expense,
        category: TransactionCategory = TransactionCategory.Predefined.Grocery,
        description: String? = "Weekly shop"
    ) = TransactionDomainModel(
        id = id,
        amount = amount,
        dateTime = dateTime,
        type = type,
        category = category,
        description = description,
        originId = "device"
    )

    private fun parsed(contents: String): CsvDocument.Parsed =
        assertIs<CsvDocument.Parsed>(TransactionCsv.decode(contents))

    @Test
    fun aRoundTripPreservesEveryField() {
        val original = transaction(id = 7)

        val result = parsed(TransactionCsv.encode(listOf(original)))

        assertEquals(0, result.invalidRows)
        val imported = result.transactions.single()
        assertEquals(original.amount, imported.amount)
        assertEquals(original.dateTime, imported.dateTime)
        assertEquals(original.type, imported.type)
        assertEquals(original.category, imported.category)
        assertEquals(original.description, imported.description)
    }

    @Test
    fun theHeaderIsWrittenOnce() {
        val encoded = TransactionCsv.encode(listOf(transaction(), transaction(id = 2)))

        assertEquals(1, encoded.lines().count { it == TransactionCsv.Header })
    }

    @Test
    fun valuesThatContainSeparatorsAreQuoted() {
        val result = parsed(
            TransactionCsv.encode(
                listOf(
                    transaction(description = "Lunch, coffee and a \"quick\" stop")
                )
            )
        )

        assertEquals("Lunch, coffee and a \"quick\" stop", result.transactions.single().description)
    }

    @Test
    fun amountsAlwaysCarryTwoDecimalsWithoutAnExponent() {
        val encoded = TransactionCsv.encode(
            listOf(
                transaction(amount = 5.0),
                transaction(id = 2, amount = 0.1),
                transaction(id = 3, amount = 1234.5)
            )
        )

        val lines = encoded.lines().filter { line -> line.isNotBlank() }
        assertTrue(lines[1].startsWith("01/02/2026,5.00,"))
        assertTrue(lines[2].startsWith("01/02/2026,0.10,"))
        assertTrue(lines[3].startsWith("01/02/2026,1234.50,"))
    }

    @Test
    fun customCategoriesSurviveTheRoundTrip() {
        val result = parsed(
            TransactionCsv.encode(
                listOf(transaction(category = TransactionCategory.Custom("Garden")))
            )
        )

        assertEquals(TransactionCategory.Custom("Garden"), result.transactions.single().category)
    }

    @Test
    fun aBomAndCrlfAreAccepted() {
        val contents = "\uFEFFdate,amount,type,category,description\r\n" +
            "01/02/2026,10.00,expense,grocery,Shop\r\n"

        val result = parsed(contents)

        assertEquals("01/02/2026", result.transactions.single().dateTime)
        assertEquals("Shop", result.transactions.single().description)
    }

    @Test
    fun unpaddedDatesAreNormalized() {
        val contents = "date,amount,type,category,description\n" +
            "1/2/2026,10.00,expense,grocery,\n"

        val result = parsed(contents)

        assertEquals("01/02/2026", result.transactions.single().dateTime)
    }

    @Test
    fun typeAndCategoryAreMatchedCaseInsensitively() {
        val contents = "date,amount,type,category,description\n" +
            "01/02/2026,10.00,Income,Salary,\n"

        val result = parsed(contents)

        assertEquals(TransactionType.Income, result.transactions.single().type)
        assertEquals(TransactionCategory.Predefined.Salary, result.transactions.single().category)
    }

    @Test
    fun aCommaDecimalAmountIsAccepted() {
        val contents = "date,amount,type,category,description\n" +
            "01/02/2026,\"10,42\",expense,grocery,\n"

        val result = parsed(contents)

        assertEquals(10.42, result.transactions.single().amount)
    }

    @Test
    fun blankDescriptionsBecomeNull() {
        val contents = "date,amount,type,category,description\n" +
            "01/02/2026,10.00,expense,grocery,\n"

        val result = parsed(contents)

        assertEquals(null, result.transactions.single().description)
    }

    @Test
    fun aDescriptionCanContainNewlines() {
        val result = parsed(TransactionCsv.encode(listOf(transaction(description = "Line one\nLine two"))))

        assertEquals("Line one\nLine two", result.transactions.single().description)
    }

    @Test
    fun theDescriptionColumnIsOptional() {
        val contents = "date,amount,type,category\n" +
            "01/02/2026,10.00,expense,grocery\n"

        val result = parsed(contents)

        assertEquals(1, result.transactions.size)
        assertEquals(null, result.transactions.single().description)
    }

    @Test
    fun uselessRowsAreCountedInsteadOfFailingTheWholeFile() {
        val contents = "date,amount,type,category,description\n" +
            "01/02/2026,10.00,expense,grocery,Fine\n" +
            "31/02/2026,10.00,expense,grocery,Bad date\n" +
            "01/02/2026,ten,expense,grocery,Bad amount\n" +
            "01/02/2026,10.00,savings,grocery,Bad type\n" +
            "01/02/2026,10.00,expense,,Bad category\n" +
            "\n"

        val result = parsed(contents)

        assertEquals(1, result.transactions.size)
        assertEquals(4, result.invalidRows)
    }

    @Test
    fun aFileWithoutTheRequiredColumnsIsUnreadable() {
        val contents = "when,amount,type,category\n" +
            "01/02/2026,10.00,expense,grocery\n"

        assertEquals(CsvDocument.Unreadable, TransactionCsv.decode(contents))
    }

    @Test
    fun anEmptyFileIsUnreadable() {
        assertEquals(CsvDocument.Unreadable, TransactionCsv.decode(""))
    }

    @Test
    fun aHeaderOnlyFileHasNoRows() {
        val result = parsed("date,amount,type,category,description\n")

        assertEquals(0, result.transactions.size)
        assertEquals(0, result.invalidRows)
    }

    @Test
    fun surroundingWhitespaceIsRemovedFromDescriptions() {
        val result = parsed(TransactionCsv.encode(listOf(transaction(description = " padded "))))

        assertEquals("padded", result.transactions.single().description)
    }
}
