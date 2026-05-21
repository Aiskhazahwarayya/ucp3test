package com.ivy.piechart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.design.l0_system.UI

@Composable
fun LineChart(
    categoryAmounts: List<CategoryAmount>,
    modifier: Modifier = Modifier.height(220.dp).fillMaxWidth(),
    lineColor: Color = UI.colors.primary
) {
    if (categoryAmounts.isEmpty()) {
        Text(text = "No data")
        return
    }

    val amounts = categoryAmounts.map { it.amount }
    val max = (amounts.maxOrNull() ?: 1.0).coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stepX = w / (amounts.size.coerceAtLeast(1))

        val path = Path()
        amounts.forEachIndexed { index, value ->
            val x = stepX * (index + 0.5f)
            val y = h - (value / max * h).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
    }
}
