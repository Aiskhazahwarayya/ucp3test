package com.ivy.piechart

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.wallet.ui.theme.toComposeColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class BarSegment(
    val categoryName: String,
    val categoryColor: Color,
    val amount: Double,
    val date: LocalDate?
)

@Composable
fun LineChart(
    categoryAmounts: List<CategoryAmount>,
    modifier: Modifier = Modifier.height(220.dp).fillMaxWidth(),
    lineColor: Color = Color.Unspecified
) {
    if (categoryAmounts.isEmpty()) {
        Text(text = "No data")
        return
    }

    val uiColors = UI.colors
    val defaultBarColor = if (lineColor == Color.Unspecified) uiColors.primary else lineColor
    
    val barSegments = mutableListOf<BarSegment>()
    val categoryGroups = mutableMapOf<String, MutableList<BarSegment>>()
    
    categoryAmounts.forEach { item ->
        val categoryName = item.category?.name?.value?.takeIf { it.isNotBlank() } ?: "Other"
        val categoryColor = item.category?.color?.value?.toComposeColor() ?: defaultBarColor
        
        val transactionsByDate = item.associatedTransactions.groupBy { trn ->
            trn.dateTime?.atZone(ZoneId.systemDefault())?.toLocalDate()
        }
        
        if (transactionsByDate.isEmpty()) {
            val segment = BarSegment(categoryName, categoryColor, item.amount.toDouble(), null)
            categoryGroups.getOrPut(categoryName) { mutableListOf() }.add(segment)
        } else {
            transactionsByDate.forEach { (date, transactions) ->
                val amount = transactions.sumOf { it.amount }.toDouble()
                val segment = BarSegment(categoryName, categoryColor, amount, date)
                categoryGroups.getOrPut(categoryName) { mutableListOf() }.add(segment)
            }
        }
    }
    
    categoryGroups.values.forEach { segments ->
        barSegments.addAll(segments)
    }
    
    val maxAmount = (barSegments.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val spacing = 12.dp.toPx()
        val bottomPadding = 36.dp.toPx()
        val topPadding = 16.dp.toPx()
        val availableHeight = h - bottomPadding - topPadding
        
        val barCount = barSegments.size
        val totalSpacing = spacing * (barCount + 1)
        val barWidth = if (barCount > 0) (w - totalSpacing) / barCount else 0f

        val gridColor = uiColors.medium.copy(alpha = 0.16f)
        repeat(4) { row ->
            val y = topPadding + availableHeight * (row / 3f)
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(w, y),
                strokeWidth = 1f
            )
        }

        drawLine(
            color = uiColors.medium.copy(alpha = 0.4f),
            start = androidx.compose.ui.geometry.Offset(0f, topPadding + availableHeight),
            end = androidx.compose.ui.geometry.Offset(w, topPadding + availableHeight),
            strokeWidth = 1.5f
        )

        var categoryIndex = 0
        var lastCategoryName: String? = null
        
        barSegments.forEachIndexed { index, segment ->
            val isCategoryChange = segment.categoryName != lastCategoryName
            if (isCategoryChange && lastCategoryName != null) {
                categoryIndex++
            }
            lastCategoryName = segment.categoryName
            
            val barColor = segment.categoryColor
            val opacity = if (segment.date == null) 1f else {
                val dateIndex = barSegments.take(index)
                    .count { it.categoryName == segment.categoryName && it.date != null }
                (1f - (dateIndex * 0.15f)).coerceIn(0.6f, 1f)
            }
            
            val barHeight = ((segment.amount / maxAmount) * availableHeight).toFloat().coerceAtLeast(4f)
            val left = spacing + index * (barWidth + spacing)
            val top = topPadding + availableHeight - barHeight
            val bottom = topPadding + availableHeight

            drawRoundRect(
                color = barColor.copy(alpha = opacity),
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )

            drawCircle(
                color = barColor.copy(alpha = opacity),
                radius = 4.5f,
                center = androidx.compose.ui.geometry.Offset(left + barWidth / 2f, top)
            )

            drawContext.canvas.nativeCanvas.apply {
                val amountPaint = Paint().apply {
                    color = uiColors.pureInverse.toArgb()
                    textSize = 18f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                }
                val amountText = "%.0f".format(segment.amount)
                drawText(amountText, left + barWidth / 2f, top - 8f, amountPaint)
                
                if (isCategoryChange) {
                    val labelPaint = Paint().apply {
                        color = uiColors.pureInverse.toArgb()
                        textSize = 20f
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    drawText(segment.categoryName, left + barWidth / 2f, bottom + 26f, labelPaint)
                }
                
                if (segment.date != null) {
                    val datePaint = Paint().apply {
                        color = uiColors.medium.toArgb()
                        textSize = 12f
                        textAlign = Paint.Align.CENTER
                    }
                    drawText(segment.date.toString(), left + barWidth / 2f, bottom + 8f, datePaint)
                }
            }
        }
    }
}
