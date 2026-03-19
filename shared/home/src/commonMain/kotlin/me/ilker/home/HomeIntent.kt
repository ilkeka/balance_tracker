package me.ilker.home

import kotlinx.datetime.YearMonth
import me.ilker.core.Intent

sealed interface HomeIntent : Intent {
    data class SetSelectedYearMonth(
        val yearMonth: YearMonth
    ): HomeIntent
}
