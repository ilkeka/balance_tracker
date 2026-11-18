package me.ilker.home.views

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

internal val HomeCardShape = RoundedCornerShape(24.dp)
internal val HomeCardPadding = 20.dp
internal val HomeScreenPadding = 16.dp

/** Gap between month cards; keeps a sliver of the neighbouring card visible. */
internal val CardPageSpacing = 4.dp

/** Keeps the navigation chevrons from shifting between shorter and longer month names. */
internal val HomeMonthLabelMinWidth = 140.dp

/** Keeps the month navigator compact on wide screens instead of stretching edge to edge. */
internal val MonthNavigatorMaxWidth = 420.dp

/** Touch target for the month label that opens the month menu. */
internal val MonthSelectorMinHeight = 48.dp

/** Width of the month picker panel that opens from the month label. */
internal val MonthPickerWidth = 260.dp

internal const val MonthPickerColumnCount = 3
internal const val MonthPickerRowCount = 4
