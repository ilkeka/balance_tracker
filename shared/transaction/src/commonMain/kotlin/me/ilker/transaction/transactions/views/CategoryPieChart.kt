package me.ilker.transaction.transactions.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.getIcon
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

private val IconSize = 16.dp
private const val MinimumIconSweepAngle = 30f
private const val DegreesToRadians = PI / 180.0

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

    var startAngle = -90f
    val slices = categoryTotals.map { categoryTotal ->
        val sweepAngle = (categoryTotal.amount / total).toFloat() * 360f
        val slice = PieSlice(
            category = categoryTotal.category,
            color = categoryTotal.category.chartColor(),
            startAngle = startAngle,
            sweepAngle = sweepAngle
        )

        startAngle += sweepAngle

        slice
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val chartSizePx = minOf(constraints.maxWidth, constraints.maxHeight).toFloat()
        val iconRadiusPx = chartSizePx * 0.29f

        Canvas(modifier = Modifier.fillMaxSize()) {
            slices.forEach { slice ->
                val gapAngle = if (slices.size > 1 && slice.sweepAngle > 4f) 1.2f else 0f

                drawArc(
                    color = slice.color,
                    startAngle = slice.startAngle + gapAngle / 2f,
                    sweepAngle = slice.sweepAngle - gapAngle,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
            }
        }

        slices.forEach { slice ->
            if (slice.sweepAngle < MinimumIconSweepAngle) return@forEach

            val angle = (slice.startAngle + slice.sweepAngle / 2f) * DegreesToRadians
            val x = (cos(angle) * iconRadiusPx).roundToInt()
            val y = (sin(angle) * iconRadiusPx).roundToInt()

            Icon(
                imageVector = slice.category.getIcon(),
                contentDescription = null,
                tint = if (slice.color.luminance() > 0.25f) Color.Black else Color.White,
                modifier = Modifier
                    .offset { IntOffset(x = x, y = y) }
                    .size(IconSize)
            )
        }
    }
}

private data class PieSlice(
    val category: TransactionCategory,
    val color: Color,
    val startAngle: Float,
    val sweepAngle: Float
)
