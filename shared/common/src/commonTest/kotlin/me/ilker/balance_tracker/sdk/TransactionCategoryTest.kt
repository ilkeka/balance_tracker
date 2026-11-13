package me.ilker.balance_tracker.sdk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionCategoryTest {

    @Test
    fun incomeCategoriesContainOnlyOtherReimbursementAndSalary() {
        assertEquals(
            setOf(
                TransactionCategory.Predefined.Other,
                TransactionCategory.Predefined.Reimbursement,
                TransactionCategory.Predefined.Salary
            ),
            TransactionType.Income.availableCategories().toSet()
        )
    }

    @Test
    fun expenseCategoriesExcludeIncomeOnlyCategories() {
        val categories = TransactionType.Expense.availableCategories()

        assertTrue(TransactionCategory.Predefined.Other in categories)
        assertFalse(TransactionCategory.Predefined.Reimbursement in categories)
        assertFalse(TransactionCategory.Predefined.Salary in categories)
        assertEquals(
            TransactionCategory.Predefined.entries.toSet() -
                setOf(
                    TransactionCategory.Predefined.Reimbursement,
                    TransactionCategory.Predefined.Salary
                ),
            categories.toSet()
        )
    }

    @Test
    fun unavailableCategoryFallsBackToOther() {
        assertEquals(
            TransactionCategory.Predefined.Other,
            TransactionCategory.Predefined.Salary.withFallbackFor(TransactionType.Expense)
        )

        assertEquals(
            TransactionCategory.Predefined.Salary,
            TransactionCategory.Predefined.Salary.withFallbackFor(TransactionType.Income)
        )
    }

    @Test
    fun customCategoryIsNeverReplaced() {
        val customCategory = TransactionCategory.Custom("Pets")

        assertEquals(
            customCategory,
            customCategory.withFallbackFor(TransactionType.Expense)
        )

        assertEquals(
            customCategory,
            customCategory.withFallbackFor(TransactionType.Income)
        )
    }
}
