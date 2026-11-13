package me.ilker.transaction.transactions.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.transaction.transactions.TransactionState

private val LightCategoryChartColors = listOf(
    Color(0xFF2E7D32),
    Color(0xFFC62828),
    Color(0xFF1565C0),
    Color(0xFFEF6C00),
    Color(0xFF6A1B9A),
    Color(0xFF00838F),
    Color(0xFFAD1457),
    Color(0xFF9E9D24),
    Color(0xFF4E342E),
    Color(0xFF37474F),
    Color(0xFF283593),
    Color(0xFF00695C)
)

private val DarkCategoryChartColors = listOf(
    Color(0xFF81C784),
    Color(0xFFE57373),
    Color(0xFF64B5F6),
    Color(0xFFFFB74D),
    Color(0xFFBA68C8),
    Color(0xFF4DD0E1),
    Color(0xFFF06292),
    Color(0xFFDCE775),
    Color(0xFFA1887F),
    Color(0xFF90A4AE),
    Color(0xFF7986CB),
    Color(0xFF4DB6AC)
)

@Composable
internal fun TransactionCategory.chartColor(): Color {
    val isDark = isSystemInDarkTheme()

    return when (this) {
        is TransactionCategory.Predefined -> {
            val palette = if (isDark) DarkCategoryChartColors else LightCategoryChartColors

            palette[ordinal % palette.size]
        }

        is TransactionCategory.Custom -> {
            val hash = value.fold(0) { hash, character -> hash * 31 + character.code }
            val hue = (hash and 0x7FFFFFFF) % 360

            Color.hsv(
                hue = hue.toFloat(),
                saturation = if (isDark) 0.45f else 0.65f,
                value = if (isDark) 0.85f else 0.55f
            )
        }
    }
}

@Composable
internal fun CategoryPieChart(
    categoryTotals: List<TransactionState.Loaded.CategoryTotalUiModel>,
    modifier: Modifier = Modifier
) {
    val total = categoryTotals.sumOf { categoryTotal -> categoryTotal.amount }
        .takeIf { value -> value > 0.0 }
        ?: return

    val slices = categoryTotals.map { categoryTotal ->
        PieSlice(
            fraction = (categoryTotal.amount / total).toFloat(),
            color = categoryTotal.category.chartColor()
        )
    }

    Canvas(modifier = modifier) {
        var startAngle = -90f

        slices.forEach { slice ->
            val sweepAngle = slice.fraction * 360f
            val gapAngle = if (slices.size > 1 && sweepAngle > 4f) 1.2f else 0f

            drawArc(
                color = slice.color,
                startAngle = startAngle + gapAngle / 2f,
                sweepAngle = sweepAngle - gapAngle,
                useCenter = true,
                size = Size(size.width, size.height)
            )

            startAngle += sweepAngle
        }
    }
}

private data class PieSlice(
    val fraction: Float,
    val color: Color
)
