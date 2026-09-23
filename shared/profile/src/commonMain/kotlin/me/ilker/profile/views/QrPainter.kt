package me.ilker.profile.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import qrcode.raw.QRCodeProcessor
import qrcode.raw.QRCodeRawData

@Composable
internal fun rememberQrCodePainter(content: String): Painter {
    val painter = remember(content) {
        QrCodePainter(QRCodeProcessor(content).encode())
    }
    return painter
}

private class QrCodePainter(
    private val rawData: QRCodeRawData,
) : Painter() {

    private val moduleCount: Int = rawData.size

    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        val cellSize = size.minDimension / moduleCount
        val startX = (size.width - cellSize * moduleCount) / 2f
        val startY = (size.height - cellSize * moduleCount) / 2f

        for (row in 0 until moduleCount) {
            for (col in 0 until moduleCount) {
                if (rawData[row][col].dark) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(startX + col * cellSize, startY + row * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}