package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Signature DetoxWall Shield-Heart Logo as displayed in the brand mockup:
 * A modern rounded shield with vibrant cyan gradient outline and a heart at the center.
 */
@Composable
fun DetoxWallLogo(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    primaryColor: Color = Color(0xFF00E5BE),
    secondaryColor: Color = Color(0xFF14B8A6)
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Shield path
            val shieldPath = Path().apply {
                moveTo(w * 0.5f, h * 0.05f)
                cubicTo(w * 0.82f, h * 0.05f, w * 0.95f, h * 0.15f, w * 0.95f, h * 0.45f)
                cubicTo(w * 0.95f, h * 0.75f, w * 0.65f, h * 0.92f, w * 0.5f, h * 0.98f)
                cubicTo(w * 0.35f, h * 0.92f, w * 0.05f, h * 0.75f, w * 0.05f, h * 0.45f)
                cubicTo(w * 0.05f, h * 0.15f, w * 0.18f, h * 0.05f, w * 0.5f, h * 0.05f)
                close()
            }

            // Fill subtle background
            drawPath(
                path = shieldPath,
                brush = Brush.radialGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.25f), Color(0xFF091E1C)),
                    center = Offset(w * 0.5f, h * 0.45f),
                    radius = w * 0.6f
                ),
                style = Fill
            )

            // Draw shield outline
            drawPath(
                path = shieldPath,
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor)
                ),
                style = Stroke(
                    width = w * 0.075f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw center heart
            val heartPath = Path().apply {
                val cx = w * 0.5f
                val cy = h * 0.46f
                val scale = w * 0.038f

                moveTo(cx, cy + 3.2f * scale)
                cubicTo(cx - 3.5f * scale, cy + 0.8f * scale, cx - 4.5f * scale, cy - 2.5f * scale, cx - 2.2f * scale, cy - 3.5f * scale)
                cubicTo(cx - 0.5f * scale, cy - 4.2f * scale, cx, cy - 2.2f * scale, cx, cy - 1.2f * scale)
                cubicTo(cx, cy - 2.2f * scale, cx + 0.5f * scale, cy - 4.2f * scale, cx + 2.2f * scale, cy - 3.5f * scale)
                cubicTo(cx + 4.5f * scale, cy - 2.5f * scale, cx + 3.5f * scale, cy + 0.8f * scale, cx, cy + 3.2f * scale)
                close()
            }

            drawPath(
                path = heartPath,
                color = primaryColor,
                style = Fill
            )
        }
    }
}
